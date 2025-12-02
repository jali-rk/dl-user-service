# User Service API Documentation

## Overview
The User Service is an internal microservice that manages user data for students and admins in the DopamineLite platform. It is called exclusively by the BFF (Backend-for-Frontend) and requires service-to-service authentication.

## Base URL
```
http://localhost:8081
```

## Authentication
All endpoints (except `/health`) require the `X-Service-Token` header:
```
X-Service-Token: <configured-internal-token>
```

## Health Check

### GET /health
Health check endpoint (no authentication required).

**Response:**
```json
{
  "status": "UP",
  "service": "userservice"
}
```

---

## Student Endpoints

### POST /students/registrations
Register a new student.

**Request Body:**
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "whatsappNumber": "+94771234567",
  "school": "Royal College",
  "address": "123 Main St, Colombo",
  "password": "SecurePassword123"
}
```

**Response:** `201 Created`
```json
{
  "user": {
    "id": "uuid",
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "whatsappNumber": "+94771234567",
    "school": "Royal College",
    "address": "123 Main St, Colombo",
    "role": "STUDENT",
    "status": "ACTIVE",
    "codeNumber": "1001",
    "isVerified": false,
    "createdAt": "2025-11-30T00:00:00Z",
    "updatedAt": "2025-11-30T00:00:00Z",
    "lastLoginAt": null
  },
  "verificationCodeGenerated": true
}
```

**Business Logic:**
- Generates sequential registration number (starting from 1001)
- Registration number is used as the initial verification code
- Verification code expires in 2 minutes
- Password is hashed with BCrypt

**Errors:**
- `400 USER_ALREADY_EXISTS` - Email already registered
- `400 VALIDATION_ERROR` - Invalid request data

---

### POST /students/verify-code
Verify student registration code.

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "code": "1001"
}
```

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "whatsappNumber": "+94771234567",
  "school": "Royal College",
  "address": "123 Main St, Colombo",
  "role": "STUDENT",
  "status": "ACTIVE",
  "codeNumber": "1001",
  "isVerified": true,
  "createdAt": "2025-11-30T00:00:00Z",
  "updatedAt": "2025-11-30T00:00:00Z",
  "lastLoginAt": null
}
```

**Business Logic:**
- Maximum 3 verification attempts
- Code expires after 2 minutes
- After 3 failed attempts, code is invalidated
- On success, user.isVerified is set to true

**Errors:**
- `400 INVALID_VERIFICATION_CODE` - Invalid or expired code
- `404 USER_NOT_FOUND` - User not found

---

### GET /students/{studentId}
Get student by ID.

**Path Parameters:**
- `studentId` (UUID) - Student's unique ID

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "whatsappNumber": "+94771234567",
  "school": "Royal College",
  "address": "123 Main St, Colombo",
  "role": "STUDENT",
  "status": "ACTIVE",
  "codeNumber": "1001",
  "isVerified": true,
  "createdAt": "2025-11-30T00:00:00Z",
  "updatedAt": "2025-11-30T00:00:00Z",
  "lastLoginAt": "2025-11-30T01:00:00Z"
}
```

**Errors:**
- `404 USER_NOT_FOUND` - Student not found or not a student role

---

### PATCH /students/{studentId}
Update student profile.

**Path Parameters:**
- `studentId` (UUID) - Student's unique ID

**Request Body:** (all fields optional)
```json
{
  "fullName": "John Updated Doe",
  "whatsappNumber": "+94771234568",
  "school": "New School",
  "address": "New Address"
}
```

**Response:** `200 OK`
Returns updated UserPublicView

**Note:** Email, role, status, and codeNumber cannot be changed through this endpoint.

**Errors:**
- `404 USER_NOT_FOUND` - Student not found
- `400 VALIDATION_ERROR` - Invalid request data

---

## Admin Endpoints

### POST /admins
Create a new admin.

**Request Body:**
```json
{
  "fullName": "Admin User",
  "email": "admin@example.com",
  "role": "ADMIN",
  "password": "SecurePassword123"
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "fullName": "Admin User",
  "email": "admin@example.com",
  "whatsappNumber": null,
  "school": null,
  "address": null,
  "role": "ADMIN",
  "status": "ACTIVE",
  "codeNumber": null,
  "isVerified": true,
  "createdAt": "2025-11-30T00:00:00Z",
  "updatedAt": "2025-11-30T00:00:00Z",
  "lastLoginAt": null
}
```

**Business Logic:**
- Admins are automatically verified
- Role can be ADMIN or MAIN_ADMIN
- codeNumber is null for admins

**Errors:**
- `400 USER_ALREADY_EXISTS` - Email already registered
- `400 INVALID_ARGUMENT` - Invalid role (must be ADMIN or MAIN_ADMIN)

---

### GET /admins/{adminId}
Get admin by ID.

**Path Parameters:**
- `adminId` (UUID) - Admin's unique ID

**Response:** `200 OK`
Returns UserPublicView

**Errors:**
- `404 USER_NOT_FOUND` - Admin not found or not an admin role

---

### PATCH /admins/{adminId}
Update admin profile.

**Path Parameters:**
- `adminId` (UUID) - Admin's unique ID

**Request Body:**
```json
{
  "fullName": "Updated Admin Name"
}
```

**Response:** `200 OK`
Returns updated UserPublicView

**Errors:**
- `404 USER_NOT_FOUND` - Admin not found

---

### GET /admins
List admins with optional filters.

**Query Parameters:**
- `role` (optional) - Filter by role (ADMIN, MAIN_ADMIN)
- `status` (optional) - Filter by status (ACTIVE, INACTIVE, SUSPENDED)

**Examples:**
- `GET /admins` - Returns all ADMIN role users
- `GET /admins?role=MAIN_ADMIN` - Returns all MAIN_ADMIN users
- `GET /admins?role=ADMIN&status=ACTIVE` - Returns active ADMIN users

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "fullName": "Admin User",
    "email": "admin@example.com",
    "role": "ADMIN",
    "status": "ACTIVE",
    "isVerified": true,
    ...
  }
]
```

---

## User Endpoints

### GET /users/{userId}
Get user by ID (any role).

**Path Parameters:**
- `userId` (UUID) - User's unique ID

**Response:** `200 OK`
Returns UserPublicView

**Errors:**
- `404 USER_NOT_FOUND` - User not found

---

### GET /users/by-email
Get user by email.

**Query Parameters:**
- `email` (required) - User's email address

**Example:**
`GET /users/by-email?email=john.doe@example.com`

**Response:** `200 OK`
Returns UserPublicView

**Errors:**
- `404 USER_NOT_FOUND` - User not found

---

## Internal Auth Endpoints

### POST /internal/auth/validate-credentials
Validate user credentials for login.

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePassword123"
}
```

**Response:** `200 OK`
```json
{
  "valid": true,
  "user": {
    "id": "uuid",
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "status": "ACTIVE",
    "isVerified": true,
    ...
  }
}
```

**Business Logic:**
- Validates email and password using BCrypt
- Ensures user status is ACTIVE
- For students, ensures isVerified is true
- Updates lastLoginAt timestamp on success

**Errors:**
- `401 INVALID_CREDENTIALS` - Invalid email or password
- `403 USER_NOT_VERIFIED` - Student account not verified

---

### POST /internal/auth/password-reset/request
Request password reset.

**Request Body:**
```json
{
  "email": "john.doe@example.com"
}
```

**Response:** `200 OK`
```json
{
  "message": "If the email exists, password reset instructions have been sent",
  "token": "raw-token-for-bff-to-send"
}
```

**Business Logic:**
- Generates random password reset token
- Token expires in 30 minutes
- Always returns success (even if user not found) to avoid leaking user existence
- BFF should send the token via email

**Note:** The `token` field is only returned when the user exists. BFF should handle email sending.

---

### POST /internal/auth/password-reset/confirm
Confirm password reset with token.

**Request Body:**
```json
{
  "token": "raw-token-from-email-link",
  "newPassword": "NewSecurePassword123"
}
```

**Response:** `200 OK`
No body

**Business Logic:**
- Validates token (not expired, not used)
- Updates user password with BCrypt hash
- Marks token as used

**Errors:**
- `400 INVALID_PASSWORD_RESET_TOKEN` - Invalid or expired token
- `404 USER_NOT_FOUND` - User not found

---

## Error Response Format

All errors follow the ErrorObject schema:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": {
    "field1": "error detail 1",
    "field2": "error detail 2"
  }
}
```

### Error Codes
- `USER_ALREADY_EXISTS` - 400
- `USER_NOT_FOUND` - 404
- `INVALID_VERIFICATION_CODE` - 400
- `INVALID_CREDENTIALS` - 401
- `USER_NOT_VERIFIED` - 403
- `INVALID_PASSWORD_RESET_TOKEN` - 400
- `INVALID_ARGUMENT` - 400
- `VALIDATION_ERROR` - 400 (includes field-level validation details)
- `INTERNAL_SERVER_ERROR` - 500
- `UNAUTHORIZED` - 401 (missing or invalid service token)

---

## Configuration

### Environment Variables
- `INTERNAL_SERVICE_TOKEN` - Shared secret for service-to-service authentication
- `SPRING_DATASOURCE_URL` - PostgreSQL database URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

### Application Properties
See `application.properties` for all configuration options including:
- Verification code expiry (default: 2 minutes)
- Password reset token expiry (default: 30 minutes)
- Maximum verification retries (default: 3)

---

## Database Schema

### Users Table
- Stores both students and admins
- Soft delete support (deletedAt field)
- Sequential codeNumber for students (from sequence)

### Verification Codes Table
- Stores registration verification codes
- 2-minute expiration
- Maximum 3 retry attempts

### Password Reset Tokens Table
- Stores password reset tokens (hashed)
- 30-minute expiration
- One-time use

---

## Notes

1. **Registration Number = Verification Code**: For initial student registration, the registration number (codeNumber) is the same as the verification code.

2. **Soft Delete**: All user queries exclude soft-deleted users (deletedAt IS NULL).

3. **Password Security**: All passwords are hashed using BCrypt. Never log or expose password hashes.

4. **Service Authentication**: All endpoints (except /health) require the X-Service-Token header.

5. **Email Notifications**: The service logs TODO messages for email notifications. Integration with Notification Service is pending.

6. **Transaction Safety**: Student registration is atomic - if verification code creation fails, the entire transaction rolls back.

