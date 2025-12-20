# Context Path Configuration - ALB Multi-Service Setup

## Problem Statement

When multiple microservices run behind the same Application Load Balancer (ALB), they cannot all use root-level paths like `/health`, `/students`, etc., as this causes routing conflicts. The ALB needs unique paths to route requests to the correct service.

## Solution Implemented

Added a **context path** (`server.servlet.context-path`) to the User Service to namespace all its endpoints under `/userservice`.

---

## Changes Made

### 1. Development Configuration
**File**: `src/main/resources/application-dev.yml`

```yaml
server:
  port: 8080
  servlet:
    context-path: /userservice
```

### 2. Production Configuration
**File**: `src/main/resources/application-prod.yml`

```yaml
server:
  port: 8080
  servlet:
    context-path: /userservice
```

### 3. Test Configuration
**File**: `src/test/resources/application-test.properties`

```properties
server.servlet.context-path=/userservice
```

---

## Impact on Endpoints

### Before (Root Context)
```
http://localhost:8080/health
http://localhost:8080/students/registrations
http://localhost:8080/students/verify-code
http://localhost:8080/students/{studentId}
http://localhost:8080/admins
http://localhost:8080/admins/{adminId}
http://localhost:8080/users/{userId}
http://localhost:8080/users/email/{email}
http://localhost:8080/internal/auth/validate-credentials
http://localhost:8080/internal/auth/password-reset/request
http://localhost:8080/internal/auth/password-reset/confirm
```

### After (With Context Path)
```
http://localhost:8080/userservice/health
http://localhost:8080/userservice/students/registrations
http://localhost:8080/userservice/students/verify-code
http://localhost:8080/userservice/students/{studentId}
http://localhost:8080/userservice/admins
http://localhost:8080/userservice/admins/{adminId}
http://localhost:8080/userservice/users/{userId}
http://localhost:8080/userservice/users/email/{email}
http://localhost:8080/userservice/internal/auth/validate-credentials
http://localhost:8080/userservice/internal/auth/password-reset/request
http://localhost:8080/userservice/internal/auth/password-reset/confirm
```

**New Resend Endpoint** (from your plan):
```
http://localhost:8080/userservice/auth/resend-verification-code
```

---

## ALB Routing Configuration

### Path-Based Routing Rules

Assuming you have multiple services behind the same ALB:

| Service | ALB Path Pattern | Target Service |
|---------|------------------|----------------|
| User Service | `/userservice/*` | userservice:8080 |
| Product Service | `/api/products/*` | productservice:8080 |
| Order Service | `/api/orders/*` | orderservice:8080 |
| Payment Service | `/api/payments/*` | paymentservice:8080 |

### Example ALB Listener Rules (AWS)

```hcl
# Terraform example
resource "aws_lb_listener_rule" "userservice" {
  listener_arn = aws_lb_listener.main.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.userservice.arn
  }

  condition {
    path_pattern {
      values = ["/userservice/*"]
    }
  }
}
```

### Health Check Configuration

The ALB health check should now target:
```
Path: /userservice/health
Port: 8080
Protocol: HTTP
Healthy threshold: 2
Unhealthy threshold: 3
Timeout: 5 seconds
Interval: 30 seconds
Success codes: 200
```

**Note**: You have two health endpoints available:
1. **Custom Health**: `/userservice/health` - Simple, fast, recommended for ALB
2. **Actuator Health**: `/userservice/actuator/health` - Detailed, for monitoring

See `HEALTH_ENDPOINTS_GUIDE.md` for complete details on both endpoints.

---

## BFF Integration Changes Required

⚠️ **Important**: Your BFF (Backend-for-Frontend) service must be updated to include the new base path.

### Before
```typescript
// BFF calling User Service
const response = await fetch('http://userservice:8080/students/registrations', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Service-Token': process.env.INTERNAL_SERVICE_TOKEN
  },
  body: JSON.stringify(studentData)
});
```

### After
```typescript
// BFF calling User Service with context path
const USER_SERVICE_URL = process.env.USER_SERVICE_URL || 'http://userservice:8080/userservice';

const response = await fetch(`${USER_SERVICE_URL}/students/registrations`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Service-Token': process.env.INTERNAL_SERVICE_TOKEN
  },
  body: JSON.stringify(studentData)
});
```

### Configuration Pattern (Recommended)
```typescript
// config/services.ts
export const SERVICES = {
  USER_SERVICE: {
    baseUrl: process.env.USER_SERVICE_URL || 'http://userservice:8080/userservice',
    endpoints: {
      registerStudent: '/students/registrations',
      verifyCode: '/students/verify-code',
      resendCode: '/auth/resend-verification-code',
      getStudent: (id: string) => `/students/${id}`,
      updateStudent: (id: string) => `/students/${id}`,
      validateCredentials: '/internal/auth/validate-credentials',
      // ... other endpoints
    }
  },
  // Other services...
};

// Usage
const url = `${SERVICES.USER_SERVICE.baseUrl}${SERVICES.USER_SERVICE.endpoints.registerStudent}`;
```

---

## Testing Impact

### Integration Tests
All integration tests automatically use the context path because of the test configuration. **No changes needed** to test code.

Example:
```java
// This still works - Spring Boot automatically prepends context path
mockMvc.perform(post("/students/registrations")
    .contentType(MediaType.APPLICATION_JSON)
    .content(requestJson))
    .andExpect(status().isCreated());
```

Spring Test internally resolves this to `/userservice/students/registrations`.

### Manual Testing with Postman/curl

Update your saved requests:

**Old**:
```bash
curl -X POST http://localhost:8080/students/registrations \
  -H "Content-Type: application/json" \
  -H "X-Service-Token: your-token" \
  -d '{"fullName":"John Doe","email":"john@example.com",...}'
```

**New**:
```bash
curl -X POST http://localhost:8080/userservice/students/registrations \
  -H "Content-Type: application/json" \
  -H "X-Service-Token: your-token" \
  -d '{"fullName":"John Doe","email":"john@example.com",...}'
```

---

## Docker Compose Considerations

If you're using Docker Compose for local development:

```yaml
version: '3.8'

services:
  userservice:
    build: ./userservice
    ports:
      - "8081:8080"  # Map to different external port
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_URL=jdbc:postgresql://postgres:5432/userservice_db
      - INTERNAL_SERVICE_TOKEN=dev-service-token
    # Context path is configured in application.yml, no need to override

  bff:
    build: ./bff
    ports:
      - "3000:3000"
    environment:
      - USER_SERVICE_URL=http://userservice:8080/userservice
      - INTERNAL_SERVICE_TOKEN=dev-service-token
    depends_on:
      - userservice
```

---

## Kubernetes/ECS Considerations

### Ingress Configuration (Kubernetes)
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: api-ingress
spec:
  rules:
  - host: api.yourdomain.com
    http:
      paths:
      - path: /userservice
        pathType: Prefix
        backend:
          service:
            name: userservice
            port:
              number: 8080
      - path: /api/products
        pathType: Prefix
        backend:
          service:
            name: productservice
            port:
              number: 8080
```

### ECS Task Definition
```json
{
  "containerDefinitions": [
    {
      "name": "userservice",
      "image": "userservice:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/userservice/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3
      }
    }
  ]
}
```

---

## Alternative Approaches (Not Recommended)

### 1. Port-Based Routing
- Each service on different port
- ALB routes by port number
- ❌ Less flexible, harder to manage

### 2. Host-Based Routing
- Each service on different subdomain
- `users.api.example.com`, `products.api.example.com`
- ❌ Requires DNS management, SSL certificates per subdomain

### 3. No Context Path + ALB Path Rewriting
- ALB strips `/userservice` prefix before forwarding
- Service still uses root paths
- ❌ Complex configuration, harder to debug

---

## Verification Steps

### 1. Local Testing
```bash
# Start the service
./mvnw spring-boot:run

# Test health endpoint
curl http://localhost:8080/userservice/health

# Expected response:
{
  "status": "UP",
  "service": "userservice"
}
```

### 2. Docker Testing
```bash
# Build image
docker build -t userservice:latest .

# Run container
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev userservice:latest

# Test from host
curl http://localhost:8080/userservice/health
```

### 3. Integration Test
```bash
# Run all tests (they automatically use context path)
./mvnw test

# All tests should pass without modification
```

---

## Rollback Plan

If you need to revert to root context:

1. Remove `server.servlet.context-path` from all config files
2. Redeploy services
3. Update BFF to use root paths
4. Update ALB routing rules

**Files to modify**:
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `src/test/resources/application-test.properties`

---

## Best Practices

### ✅ Do's
1. **Use consistent naming**: `/api/{service-name}` pattern
2. **Document all endpoints**: Update API documentation with context path
3. **Environment variables**: Make base URL configurable in BFF
4. **Health checks**: Always include context path in health check configuration
5. **Logging**: Log full URLs in BFF for debugging

### ❌ Don'ts
1. **Don't hardcode URLs**: Use environment variables for service URLs
2. **Don't mix patterns**: Keep context path consistent across environments
3. **Don't forget documentation**: Update all API docs and Postman collections
4. **Don't skip testing**: Test all endpoints after adding context path

---

## Monitoring & Debugging

### Logs to Check
```bash
# Service startup - verify context path
tail -f logs/userservice.log | grep "context-path"

# Expected log:
# Tomcat started on port(s): 8080 (http) with context path '/userservice'
```

### Common Issues

**Issue**: 404 Not Found after adding context path
- **Cause**: BFF or client still using old root paths
- **Solution**: Update all service calls to include `/userservice`

**Issue**: Health check failing in ALB
- **Cause**: Health check path not updated
- **Solution**: Update target group health check to `/userservice/health`

**Issue**: Tests failing
- **Cause**: Test config missing context path
- **Solution**: Add `server.servlet.context-path=/userservice` to test properties

---

## Summary

✅ **Problem Solved**: User Service now has unique path `/userservice/*`  
✅ **ALB Compatible**: Can route to multiple services on same ALB  
✅ **Tests Updated**: All configurations include context path  
✅ **Backwards Compatible**: No changes to controller code needed  
✅ **Health Check**: Now accessible at `/userservice/health`

**Next Steps**:
1. ✅ Context path configured (DONE)
2. ⏳ Update BFF to use new base path
3. ⏳ Update ALB routing rules
4. ⏳ Update API documentation
5. ⏳ Update Postman collections
6. ⏳ Test end-to-end flow

---

**Document Version**: 1.0  
**Last Updated**: December 21, 2025  
**Status**: Configuration Applied

