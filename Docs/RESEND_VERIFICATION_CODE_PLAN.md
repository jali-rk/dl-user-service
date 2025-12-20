# Resend Verification Code Feature - Implementation Plan

## Executive Summary
Implement a public authentication endpoint `POST /auth/resend-verification-code` that allows unverified students to request a new 6-digit verification code when their original code has expired or wasn't received.

## Current System Analysis

### Existing Registration Flow
1. **Student Registration** → `POST /students/registrations`
   - Sequential registration number generated (e.g., "1001", "1002")
   - Registration number used as initial verification code
   - Code expires in 2 minutes
   - User created with `isVerified = false`

2. **Code Verification** → `POST /students/verify-code`
   - Accepts email + code
   - Validates code hasn't expired, been consumed, or exceeded retries (max 3)
   - Sets user's `isVerified = true`
   - Marks code as consumed

### Architecture Patterns Observed
- **Layered Architecture**: Controller → Service → Repository
- **DTO Pattern**: Request/Response DTOs with Jakarta validation
- **Exception Handling**: Custom exceptions with global handler
- **Security**: Generic responses for sensitive flows (password reset returns success even if user doesn't exist)
- **Transaction Management**: `@Transactional` annotations on service methods
- **Logging**: Comprehensive logging at INFO/WARN/DEBUG levels

### Similar Feature: Password Reset
- Request endpoint returns generic success (security best practice)
- Generates token and stores hash
- Returns token in response for BFF to send via email
- 30-minute expiration
- Marks old tokens as used

---

## Design Decisions

### 1. Controller Location: NEW `AuthController`
**Decision**: Create new `AuthController` at `/auth` path

**Rationale**:
- This is a **public authentication flow**, not student-specific operations
- `StudentController` (`/students/*`) is for student profile management
- `InternalAuthController` (`/internal/auth/*`) is for BFF-only internal calls
- `/auth/*` aligns with REST conventions for public auth endpoints
- Future auth endpoints (login, logout) would logically go here
- With service context path `/userservice`, full path will be `/userservice/auth/resend-verification-code`

**Full Path**: `/userservice/auth/resend-verification-code` (with context path)

### 2. Response Format: Nested Structure
**Decision**: Follow the specified nested format

```json
{
  "success": true,
  "data": {
    "message": "Verification code has been resent to your email",
    "codeSent": true
  }
}
```

**Rationale**:
- Matches the exact requirement specification
- Provides standard API response envelope pattern
- `success` flag enables easy client-side handling
- `data` wrapping allows consistent response structure
- Differs from some existing DTOs but acceptable for new controller

### 3. Code Generation: 6-Digit Random
**Decision**: Use `SecureRandom` to generate 6-digit numeric codes

**Implementation**:
```java
SecureRandom secureRandom = new SecureRandom();
String code = String.format("%06d", secureRandom.nextInt(1000000));
```

**Rationale**:
- Different from registration number pattern (sequential)
- Cryptographically secure random generation
- 6 digits = 1,000,000 combinations (sufficient for short-lived codes)
- Easy to type and read (numeric only)
- 2-minute expiration limits brute force risk

### 4. Old Code Invalidation: Mark as Consumed
**Decision**: Explicitly mark old verification codes as consumed

**Rationale**:
- Provides clear audit trail
- Consistent with password reset pattern
- Prevents confusion if user has multiple codes
- Database explicitly shows code lifecycle
- Query optimization: `findLatestActiveByUserIdAndType` already filters consumed codes

### 5. Security: Generic Success Response
**Decision**: Return generic success even if user not found

**Rationale**:
- Prevents email enumeration attacks
- Consistent with password reset security pattern
- Logs actual result internally for monitoring
- Only returns error if user exists but is already verified (acceptable leak)

### 6. Rate Limiting: Deferred to Future
**Decision**: No rate limiting in initial implementation

**Rationale**:
- Keep initial implementation simple
- 2-minute expiration naturally limits abuse
- Can be added in future iteration with:
  - `resendCount` field on `VerificationCode`
  - `lastResendAt` timestamp
  - Business rule: max 5 resends per hour per user
- Monitor production usage before implementing

---

## Implementation Plan

### Phase 1: DTO Layer

#### 1.1 Create `ResendVerificationCodeRequest.java`
**Location**: `src/main/java/com/dopamine/userservice/dto/ResendVerificationCodeRequest.java`

```java
package com.dopamine.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resending verification code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendVerificationCodeRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
```

#### 1.2 Create `ResendVerificationCodeResponse.java`
**Location**: `src/main/java/com/dopamine/userservice/dto/ResendVerificationCodeResponse.java`

```java
package com.dopamine.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for resend verification code request.
 * Follows the standard API response format with success and data wrapper.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendVerificationCodeResponse {
    
    private boolean success;
    private ResendVerificationCodeData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResendVerificationCodeData {
        private String message;
        private boolean codeSent;
    }
}
```

---

### Phase 2: Exception Layer

#### 2.1 Create `UserAlreadyVerifiedException.java`
**Location**: `src/main/java/com/dopamine/userservice/exception/UserAlreadyVerifiedException.java`

```java
package com.dopamine.userservice.exception;

/**
 * Exception thrown when attempting to resend verification code to already verified user.
 */
public class UserAlreadyVerifiedException extends RuntimeException {
    public UserAlreadyVerifiedException(String message) {
        super(message);
    }
}
```

#### 2.2 Update `GlobalExceptionHandler.java`
**Location**: `src/main/java/com/dopamine/userservice/exception/GlobalExceptionHandler.java`

**Add new handler method**:
```java
@ExceptionHandler(UserAlreadyVerifiedException.class)
public ResponseEntity<ErrorObject> handleUserAlreadyVerified(UserAlreadyVerifiedException ex) {
    log.warn("User already verified: {}", ex.getMessage());
    ErrorObject error = ErrorObject.builder()
            .code("USER_ALREADY_VERIFIED")
            .message(ex.getMessage())
            .build();
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

---

### Phase 3: Service Layer

#### 3.1 Update `UserService.java` Interface
**Location**: `src/main/java/com/dopamine/userservice/service/UserService.java`

**Add method signature**:
```java
/**
 * Resend verification code to unverified student.
 * Returns generic success response even if user not found (security measure).
 */
ResendVerificationCodeResponse resendVerificationCode(ResendVerificationCodeRequest request);
```

#### 3.2 Implement in `UserServiceImpl.java`
**Location**: `src/main/java/com/dopamine/userservice/service/impl/UserServiceImpl.java`

**Add import**:
```java
import java.security.SecureRandom;
```

**Add implementation** (after `verifyStudentCode` method):
```java
@Override
@Transactional
public ResendVerificationCodeResponse resendVerificationCode(ResendVerificationCodeRequest request) {
    log.info("Resend verification code requested for email: {}", request.getEmail());
    
    try {
        // Find user by email
        User user = userRepository.findByEmailIgnoreCaseAndNotDeleted(request.getEmail())
                .orElse(null);
        
        if (user != null) {
            // Validate user is a student
            if (user.getRole() != Role.STUDENT) {
                log.warn("Resend verification code attempted for non-student user: {}", user.getId());
                throw new IllegalArgumentException("Verification code resend is only available for students");
            }
            
            // Check if user is already verified
            if (user.isVerified()) {
                log.info("User {} is already verified", user.getId());
                throw new UserAlreadyVerifiedException("User account is already verified");
            }
            
            // Mark old verification code(s) as consumed
            verificationCodeRepository.findLatestActiveByUserIdAndType(
                user.getId(), 
                VerificationType.REGISTRATION, 
                Instant.now()
            ).ifPresent(oldCode -> {
                oldCode.markAsConsumed();
                verificationCodeRepository.save(oldCode);
                log.debug("Marked old verification code as consumed for user: {}", user.getId());
            });
            
            // Generate new 6-digit random code
            SecureRandom secureRandom = new SecureRandom();
            String newCode = String.format("%06d", secureRandom.nextInt(1000000));
            log.debug("Generated new verification code for user: {}", user.getId());
            
            // Create new verification code
            VerificationCode verificationCode = VerificationCode.builder()
                    .userId(user.getId())
                    .code(newCode)
                    .type(VerificationType.REGISTRATION)
                    .expiresAt(Instant.now().plus(Duration.ofMinutes(2)))
                    .retryCount(0)
                    .build();
            
            verificationCodeRepository.save(verificationCode);
            log.info("Created new verification code for user: {}", user.getId());
            
            // TODO: Send new verification code to email
            log.info("TODO: Send verification code {} to email {}", newCode, user.getEmail());
            
            return ResendVerificationCodeResponse.builder()
                    .success(true)
                    .data(ResendVerificationCodeResponse.ResendVerificationCodeData.builder()
                            .message("Verification code has been resent to your email")
                            .codeSent(true)
                            .build())
                    .build();
        } else {
            log.info("Resend verification code requested for non-existent email: {}", request.getEmail());
        }
        
    } catch (UserAlreadyVerifiedException | IllegalArgumentException e) {
        // Re-throw these exceptions to be handled by controller advice
        throw e;
    } catch (Exception e) {
        log.error("Unexpected error during resend verification code", e);
    }
    
    // Return generic success for security (don't reveal if user exists)
    return ResendVerificationCodeResponse.builder()
            .success(true)
            .data(ResendVerificationCodeResponse.ResendVerificationCodeData.builder()
                    .message("If the email exists and is unverified, a verification code has been sent")
                    .codeSent(false) // Generic response doesn't confirm
                    .build())
            .build();
}
```

---

### Phase 4: Controller Layer

#### 4.1 Create `AuthController.java`
**Location**: `src/main/java/com/dopamine/userservice/controller/AuthController.java`

```java
package com.dopamine.userservice.controller;

import com.dopamine.userservice.dto.ResendVerificationCodeRequest;
import com.dopamine.userservice.dto.ResendVerificationCodeResponse;
import com.dopamine.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for public authentication operations.
 * Handles verification code resend and other public auth flows.
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Resend verification code to unverified student.
     * POST /auth/resend-verification-code
     * 
     * Returns generic success even if user not found (security measure).
     * Only returns error if user exists but is already verified.
     */
    @PostMapping("/resend-verification-code")
    public ResponseEntity<ResendVerificationCodeResponse> resendVerificationCode(
            @Valid @RequestBody ResendVerificationCodeRequest request) {
        log.info("Resend verification code request for email: {}", request.getEmail());
        ResendVerificationCodeResponse response = userService.resendVerificationCode(request);
        return ResponseEntity.ok(response);
    }
}
```

---

### Phase 5: Testing

#### 5.1 Create `AuthControllerIntegrationTest.java`
**Location**: `src/test/java/com/dopamine/userservice/controller/AuthControllerIntegrationTest.java`

**Test Cases**:
1. ✅ **Successful resend for unverified student**
   - Register student, resend code, verify response format
   - Assert new code is different from registration number
   - Assert old code marked as consumed
   - Assert new code expires in ~2 minutes

2. ✅ **User already verified**
   - Register and verify student
   - Attempt resend → Expect 403 FORBIDDEN with USER_ALREADY_VERIFIED

3. ✅ **Non-existent user**
   - Send resend request for non-existent email
   - Assert generic success response (security)

4. ✅ **Non-student user (admin)**
   - Create admin user (unverified hypothetically)
   - Attempt resend → Expect 400 BAD_REQUEST

5. ✅ **Multiple resend requests**
   - Resend twice for same user
   - Verify second code invalidates first
   - Verify both codes are persisted with different timestamps

6. ✅ **Code expiration validation**
   - Resend code, wait >2 minutes (mock time)
   - Verify code is expired using `isExpired()` method

7. ✅ **Validation errors**
   - Invalid email format → 400 with validation error
   - Missing email → 400 with validation error

#### 5.2 Update `UserServiceImplTest.java`
**Location**: `src/test/java/com/dopamine/userservice/service/UserServiceImplTest.java`

**Test Cases**:
1. ✅ **Successful resend - code generation**
   - Verify code is 6 digits (000000-999999)
   - Verify code is numeric only
   - Verify uses SecureRandom (can't test randomness, but verify format)

2. ✅ **Successful resend - old code invalidation**
   - Mock existing active code
   - Verify `markAsConsumed()` called
   - Verify repository saves consumed code

3. ✅ **User not found - generic response**
   - Mock repository returns empty
   - Verify generic success response returned
   - Verify no exception thrown

4. ✅ **User already verified - exception**
   - Mock user with `isVerified = true`
   - Verify `UserAlreadyVerifiedException` thrown

5. ✅ **Non-student user - exception**
   - Mock user with `role = ADMIN`
   - Verify `IllegalArgumentException` thrown

6. ✅ **Code expiration set correctly**
   - Verify `expiresAt` is ~2 minutes in future
   - Verify tolerance (±1 second)

7. ✅ **Repository interactions**
   - Verify `findByEmailIgnoreCaseAndNotDeleted` called once
   - Verify `findLatestActiveByUserIdAndType` called once
   - Verify `save` called twice (old consumed, new created)

---

## Database Considerations

### No Schema Changes Required ✅
- `verification_codes` table already supports this flow
- `type = REGISTRATION` reused for resent codes
- `consumedAt` marks old codes as invalid
- `expiresAt` enforces 2-minute window
- `retryCount` tracks failed verification attempts

### Data Flow
```
User registers → code "1001" created
User requests resend → code "1001" marked consumed, new code "384756" created
User verifies with "384756" → code marked consumed, user.isVerified = true
```

---

## Security Considerations

### ✅ Email Enumeration Prevention
- Generic success response for non-existent users
- Logs track actual results for monitoring
- Consistent timing (doesn't reveal if user exists)

### ✅ Brute Force Mitigation
- 2-minute expiration window
- 3 retry limit on verification (existing)
- 6-digit code = 1M combinations
- Future: Rate limiting on resend requests

### ✅ Code Security
- `SecureRandom` for cryptographic strength
- Codes stored in database (not in JWT or client)
- Old codes explicitly invalidated
- Type-specific codes (REGISTRATION vs EMAIL_CHANGE)

### ⚠️ Future Enhancements
1. **Rate Limiting**: Max 5 resends per hour per email
2. **IP-based Throttling**: Prevent distributed attacks
3. **CAPTCHA**: Add after N failed verification attempts
4. **Audit Log**: Track all resend requests for security monitoring

---

## API Documentation Update

### Endpoint: POST /auth/resend-verification-code

**Description**: Request a new verification code for unverified student accounts. Use this when the original code has expired or wasn't received.

**Authentication**: Public (no token required)

**Request**:
```json
{
  "email": "john@example.com"
}
```

**Responses**:

**200 OK - Success** (user found and code sent):
```json
{
  "success": true,
  "data": {
    "message": "Verification code has been resent to your email",
    "codeSent": true
  }
}
```

**200 OK - Generic Success** (user not found - security):
```json
{
  "success": true,
  "data": {
    "message": "If the email exists and is unverified, a verification code has been sent",
    "codeSent": false
  }
}
```

**403 FORBIDDEN - Already Verified**:
```json
{
  "code": "USER_ALREADY_VERIFIED",
  "message": "User account is already verified"
}
```

**400 BAD REQUEST - Non-Student User**:
```json
{
  "code": "INVALID_ARGUMENT",
  "message": "Verification code resend is only available for students"
}
```

**400 BAD REQUEST - Validation Error**:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": {
    "email": "Email must be valid"
  }
}
```

---

## Implementation Checklist

### Files to Create
- [ ] `src/main/java/com/dopamine/userservice/dto/ResendVerificationCodeRequest.java`
- [ ] `src/main/java/com/dopamine/userservice/dto/ResendVerificationCodeResponse.java`
- [ ] `src/main/java/com/dopamine/userservice/exception/UserAlreadyVerifiedException.java`
- [ ] `src/main/java/com/dopamine/userservice/controller/AuthController.java`
- [ ] `src/test/java/com/dopamine/userservice/controller/AuthControllerIntegrationTest.java`

### Files to Modify
- [ ] `src/main/java/com/dopamine/userservice/exception/GlobalExceptionHandler.java` - Add handler for UserAlreadyVerifiedException
- [ ] `src/main/java/com/dopamine/userservice/service/UserService.java` - Add method signature
- [ ] `src/main/java/com/dopamine/userservice/service/impl/UserServiceImpl.java` - Implement resendVerificationCode
- [ ] `src/test/java/com/dopamine/userservice/service/UserServiceImplTest.java` - Add unit tests

### Documentation to Update
- [ ] `Docs/API_DOCUMENTATION.md` - Add new endpoint documentation
- [ ] `README.md` - Update feature list if applicable

### Testing Checklist
- [ ] Unit tests pass for service layer
- [ ] Integration tests pass for controller
- [ ] Manual testing with Postman/curl
- [ ] Edge case validation (expired codes, multiple resends)
- [ ] Error response format validation

### Code Review Checklist
- [ ] Follows existing code patterns
- [ ] Proper logging at all levels
- [ ] Transaction boundaries correct
- [ ] Exception handling comprehensive
- [ ] Security considerations addressed
- [ ] Documentation complete

---

## Rollout Strategy

### Phase 1: Core Implementation ✅
- Implement all core files and logic
- Write and pass all tests
- Code review and merge

### Phase 2: Integration Testing
- Test with BFF integration
- Verify email sending flow (when implemented)
- Load testing for concurrent requests

### Phase 3: Production Deployment
- Deploy to staging environment
- Monitor logs for errors
- Verify metrics (resend rate, success rate)
- Deploy to production with monitoring

### Phase 4: Post-Launch Monitoring
- Track resend request volume
- Monitor abuse patterns
- Collect user feedback
- Plan rate limiting implementation if needed

---

## Success Metrics

### Technical Metrics
- ✅ All tests pass (unit + integration)
- ✅ No errors in logs after deployment
- ✅ Response time < 200ms (95th percentile)
- ✅ Code coverage > 80%

### Business Metrics
- Track resend request volume (should be < 20% of registrations)
- Monitor verification success rate after resend
- Track time between registration and verification
- Identify any abuse patterns requiring rate limiting

### User Experience
- Students can successfully resend codes
- Clear error messages for edge cases
- Email delivery within 30 seconds
- Smooth verification flow completion

---

## Future Enhancements

1. **Rate Limiting**
   - Add `resendCount` to track requests
   - Implement max 5 resends per hour per email
   - Return 429 Too Many Requests when limit exceeded

2. **Analytics Dashboard**
   - Resend request trends
   - Top reasons for resend (survey)
   - Verification success rate analysis

3. **Email Template Improvements**
   - Rich HTML email with code prominently displayed
   - Include expiration time in email
   - Add FAQ link for common issues

4. **SMS Fallback**
   - Option to receive code via SMS if email fails
   - Store whatsappNumber for alternative delivery

5. **Progressive Code Expiration**
   - First code: 2 minutes
   - Resent codes: 5 minutes (more generous)
   - Reduces frustration for slow email delivery

---

## Appendix: Code Review Questions

### For Reviewers
1. Is the nested response format acceptable vs. flat DTO pattern?
2. Should rate limiting be included in initial implementation?
3. Is 6-digit numeric code sufficient or prefer alphanumeric?
4. Should we return different messages for already-verified vs. not-found?
5. Is `AuthController` the right location vs. `StudentController`?

### Answers
1. **Nested format** - Follows requirement spec, acceptable for new controller
2. **Rate limiting** - Defer to future iteration, keep initial simple
3. **6-digit numeric** - Sufficient with 2-min expiration, easy to type
4. **Different messages** - Already verified returns error (acceptable leak), not-found returns generic (security)
5. **AuthController** - Correct choice, public auth flows belong here

---

## References
- Existing Code: `UserServiceImpl.registerStudent()` - Pattern for code creation
- Existing Code: `UserServiceImpl.requestPasswordReset()` - Pattern for generic security responses
- Existing Code: `VerificationCode` entity - Validation methods and lifecycle
- Spring Security Best Practices: https://spring.io/guides/topicals/spring-security-architecture
- OWASP: https://owasp.org/www-project-web-security-testing-guide/

---

**Document Version**: 1.0  
**Last Updated**: 2025-12-15  
**Author**: Development Team  
**Status**: Ready for Implementation

