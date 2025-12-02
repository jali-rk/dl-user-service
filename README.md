Proprietary - DopamineLite Platform

## License

   - Consider using async processing for emails
   - Add retry logic for failed notifications
   - Implement actual HTTP client or message queue integration
4. **Notification Service**:

   - Set up metrics collection
   - Implement proper logging aggregation
   - Add Spring Boot Actuator for health checks
3. **Monitoring**:

   - Enable SSL for PostgreSQL connections
   - Set appropriate connection limits
   - Use connection pooling (HikariCP is configured by default)
2. **Database**:

   - Consider implementing mTLS or service JWTs for better security
   - Use HTTPS for all communications
   - Set strong `INTERNAL_SERVICE_TOKEN` via environment variable
1. **Security**:

## Production Considerations

```
mvn clean compile
```bash
MapStruct generates mapper implementations at compile time. After modifying mappers, rebuild:

### Code Generation

```
mvn flyway:migrate
```bash
To run migrations manually:

```
mvn flyway:info
```bash
To check migration status:

Flyway migrations are located in `src/main/resources/db/migration/`.

### Database Migrations

```
mvn test
```bash

### Running Tests

## Development

- Password reset links via email
- Registration codes via email
**TODO**: Implement actual HTTP integration with the Notification Service to send:

Currently, the `NotificationService` is a **stub implementation** that logs messages.

## Notification Service Integration

- `UNAUTHORIZED_SERVICE` - 401
- `VALIDATION_ERROR` - 400
- `INVALID_PASSWORD_RESET_TOKEN` - 400
- `INVALID_CREDENTIALS` - 401
- `INVALID_VERIFICATION_CODE` - 400
- `USER_NOT_FOUND` - 404
- `USER_ALREADY_EXISTS` - 400
Common error codes:

```
}
  "details": {}
  "message": "Human-readable message",
  "code": "ERROR_CODE",
{
```json

All errors return a standard `ErrorObject` format:

## Error Handling

- Last login timestamp is updated on successful validation
- Students must be verified before logging in
- User must be ACTIVE status
- Email/password combination is validated

### Credential Validation

6. Token is marked as used
5. User confirms reset with the token and new password
4. BFF receives the raw token to send in the email link
3. Token is valid for **30 minutes**
2. A **random token** (UUID-based) is generated and hashed
1. User requests password reset via email

### Password Reset Flow

6. Once verified, the student can log in
5. Students have **3 attempts** to verify their code
4. The verification code expires in **2 minutes**
3. The **same value** is used as the initial verification code
2. This registration number is **permanently assigned** to the student as their `codeNumber`
1. When a student registers, a **sequential registration number** is generated (starting from 1001)

### Registration Number Flow

## Key Business Rules

```
  }'
    "school": "Test School"
    "whatsappNumber": "+1234567890",
    "password": "password123",
    "email": "john@example.com",
    "fullName": "John Doe",
  -d '{
  -H "X-Service-Token: your-service-token" \
  -H "Content-Type: application/json" \
curl -X POST http://localhost:8081/students/registrations \
```bash
Example:

All endpoints require the `X-Service-Token` header with a valid service token.

## Authentication

- `POST /internal/auth/password-reset/confirm` - Confirm password reset
- `POST /internal/auth/password-reset/request` - Request password reset
- `POST /internal/auth/validate-credentials` - Validate user credentials

### Internal Auth Endpoints

- `GET /users/by-email?email={email}` - Get user by email
- `GET /users/{userId}` - Get user by ID

### User Endpoints

- `GET /admins` - Get all admins
- `PATCH /admins/{adminId}` - Update admin profile
- `GET /admins/{adminId}` - Get admin by ID
- `POST /admins` - Create admin or main admin

### Admin Endpoints

- `GET /students` - Get all students
- `PATCH /students/{studentId}` - Update student profile
- `GET /students/{studentId}` - Get student by ID
- `POST /students/verify-code` - Verify student registration code
- `POST /students/registrations` - Register a new student

### Student Endpoints

## API Endpoints

**Important**: Set the `INTERNAL_SERVICE_TOKEN` environment variable in production!

```
user.service.internal-token=change-me-in-production
# Internal Service Authentication

spring.datasource.password=postgres
spring.datasource.username=postgres
spring.datasource.url=jdbc:postgresql://localhost:5432/userservice_db
# Database

server.port=8081
# Server
```properties

Key configuration properties in `application.properties`:

### Configuration

3. **The service will start on port 8081**

   ```
   java -jar target/userservice-0.0.1-SNAPSHOT.jar
   ```bash
   Or run the compiled JAR:

   ```
   mvn spring-boot:run
   ```bash
2. **Run the application:**

   ```
   mvn clean install
   ```bash
1. **Build the project:**

### Running the Application

Create a database named `userservice_db` and update `application.properties` with your credentials.

#### Option 2: Manual PostgreSQL Setup

- Password: `postgres`
- Username: `postgres`
- Database: `userservice_db`
This will start PostgreSQL on port 5432 with:

```
docker-compose up -d
```bash

#### Option 1: Using Docker Compose

### Database Setup

- PostgreSQL 18 (or use Docker Compose)
- Maven 3.6+
- Java 21

### Prerequisites

## Getting Started

- Optimized indexes for common queries
- Automatic timestamp management
- Case-insensitive email uniqueness
- Soft delete support (deleted_at column)
- Sequential registration numbers starting from 1001

### Key Features

4. **student_code_number_seq** - Sequence for generating student registration numbers
3. **password_reset_tokens** - Stores password reset tokens
2. **verification_codes** - Stores verification codes for registration
1. **users** - Stores all user types (students, admins, main admins)

### Tables

## Database Schema

```
    └── impl
└── service         # Service interfaces and implementations
├── security        # Security filters and config
├── repository      # Spring Data repositories
├── mapper          # MapStruct mappers
├── exception       # Custom exceptions
├── dto             # Data Transfer Objects
├── domain          # JPA entities
├── controller      # REST controllers
├── config          # Spring configuration classes
com.dopamine.userservice
```

### Package Structure

- **Repository Layer**: Data access
- **Service Layer**: Business logic
- **Controller Layer**: REST API endpoints
The service follows a standard 3-layer architecture:

## Architecture

- **Maven** for build management
- **MapStruct** for DTO mapping
- **Lombok** for reducing boilerplate
- **BCrypt** for password hashing
- **Spring Security** for service-to-service authentication
- **Flyway** for database migrations
- **PostgreSQL 18**
- **Spring Data JPA** with Hibernate
- **Spring Boot 3.4.12**
- **Java 21**

## Technology Stack

- User profile management
- Password reset flow
- Internal credential validation for the BFF
- Admin and Main Admin management
- Email-based verification for students
- Student registration with sequential registration numbers
The User Service is responsible for:

## Overview

A Spring Boot microservice that manages user data for the DopamineLite platform, including students, admins, and main admins.


