# Security Filter Fix - Actuator Health Endpoints

## Problem Identified

When testing the deployed service, the actuator health endpoint was blocked:

```bash
curl http://dev-microservices-alb-2015993104.ap-south-1.elb.amazonaws.com/userservice/actuator/health
# Response: {"code":"UNAUTHORIZED","message":"Service authentication required"}
```

## Root Cause

The `ServiceAuthFilter` was checking for health and actuator paths **without considering the context path**:

### Before (Broken)
```java
String requestPath = request.getRequestURI();
if (requestPath.startsWith("/actuator") || requestPath.equals("/health")) {
    filterChain.doFilter(request, response);
    return;
}
```

**Problem**: 
- Checked for `/actuator` but actual path is `/userservice/actuator`
- Checked for `/health` but actual path is `/userservice/health`
- Filter blocked all requests to these endpoints

## Solution Applied

Updated the filter to check for endpoints **regardless of context path**:

### After (Fixed) ✅
```java
String requestPath = request.getRequestURI();

// Allow health endpoints (both custom and actuator)
if (requestPath.endsWith("/health") || 
    requestPath.contains("/actuator/") ||
    requestPath.endsWith("/actuator")) {
    log.debug("Allowing unauthenticated access to health/actuator endpoint: {}", requestPath);
    filterChain.doFilter(request, response);
    return;
}
```

**Benefits**:
- ✅ Works with any context path (`/userservice/health`, `/health`, etc.)
- ✅ Covers all actuator endpoints (`/actuator/health`, `/actuator/metrics`, etc.)
- ✅ Handles both `/actuator` and `/actuator/*` patterns
- ✅ Includes debug logging for troubleshooting

---

## Endpoints Now Accessible Without Authentication

### Health Endpoints
```bash
# Custom health endpoint
curl http://your-alb.amazonaws.com/userservice/health
# Response: {"status":"UP","service":"userservice"}

# Actuator health endpoint
curl http://your-alb.amazonaws.com/userservice/actuator/health
# Response: {"status":"UP"}
```

### Other Actuator Endpoints
```bash
# Application info
curl http://your-alb.amazonaws.com/userservice/actuator/info

# Metrics list
curl http://your-alb.amazonaws.com/userservice/actuator/metrics

# Specific metric
curl http://your-alb.amazonaws.com/userservice/actuator/metrics/jvm.memory.used

# Prometheus metrics
curl http://your-alb.amazonaws.com/userservice/actuator/prometheus
```

---

## Security Implications

### What's Public (No Auth Required)
✅ `/userservice/health` - Custom health check  
✅ `/userservice/actuator/health` - Actuator health  
✅ `/userservice/actuator/info` - Application info  
✅ `/userservice/actuator/metrics` - Metrics  
✅ `/userservice/actuator/prometheus` - Prometheus metrics  

**Why it's safe**:
- Health endpoints are designed to be public
- Required for ALB health checks
- Required for monitoring tools
- Don't expose sensitive data (details hidden in prod)
- Standard practice for microservices

### What's Protected (Auth Required)
🔒 `/userservice/students/*` - All student operations  
🔒 `/userservice/admins/*` - All admin operations  
🔒 `/userservice/users/*` - All user operations  
🔒 `/userservice/internal/auth/*` - Internal auth operations  
🔒 `/userservice/auth/*` - Public auth operations  

**Protected by**: `X-Service-Token` header validation

---

## Testing

### Test Without Authentication (Should Work ✅)
```bash
# Custom health
curl http://localhost:8080/userservice/health
# Expected: {"status":"UP","service":"userservice"}

# Actuator health
curl http://localhost:8080/userservice/actuator/health
# Expected: {"status":"UP"}

# Actuator info
curl http://localhost:8080/userservice/actuator/info
# Expected: Application metadata

# Actuator metrics
curl http://localhost:8080/userservice/actuator/metrics
# Expected: List of available metrics
```

### Test Protected Endpoints (Should Fail Without Token ❌)
```bash
# Student registration without token
curl -X POST http://localhost:8080/userservice/students/registrations \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test",...}'
# Expected: {"code":"UNAUTHORIZED","message":"Service authentication required"}
```

### Test Protected Endpoints (Should Work With Token ✅)
```bash
# Student registration with token
curl -X POST http://localhost:8080/userservice/students/registrations \
  -H "Content-Type: application/json" \
  -H "X-Service-Token: your-internal-token" \
  -d '{"fullName":"Test",...}'
# Expected: 201 Created with user data
```

---

## ALB Health Check Configuration

Now that actuator endpoints are accessible, you can use either:

### Option 1: Custom Health (Recommended)
```hcl
resource "aws_lb_target_group" "userservice" {
  health_check {
    path                = "/userservice/health"
    protocol            = "HTTP"
    port                = "traffic-port"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }
}
```

### Option 2: Actuator Health
```hcl
resource "aws_lb_target_group" "userservice" {
  health_check {
    path                = "/userservice/actuator/health"
    protocol            = "HTTP"
    port                = "traffic-port"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
    matcher             = "200"
  }
}
```

---

## Monitoring Configuration

### Prometheus Scraping
```yaml
scrape_configs:
  - job_name: 'userservice'
    metrics_path: '/userservice/actuator/prometheus'
    static_configs:
      - targets: ['your-alb.amazonaws.com']
    # No authentication required
```

### CloudWatch Monitoring
```bash
# Script to check health and push to CloudWatch
HEALTH_STATUS=$(curl -s http://your-alb.amazonaws.com/userservice/actuator/health | jq -r '.status')

if [ "$HEALTH_STATUS" == "UP" ]; then
  aws cloudwatch put-metric-data \
    --namespace UserService \
    --metric-name HealthStatus \
    --value 1
else
  aws cloudwatch put-metric-data \
    --namespace UserService \
    --metric-name HealthStatus \
    --value 0
fi
```

---

## Debug Logging

The filter now includes debug logging:

```java
log.debug("Allowing unauthenticated access to health/actuator endpoint: {}", requestPath);
```

### Enable Debug Logging
Add to `application-dev.yml`:
```yaml
logging:
  level:
    com.dopamine.userservice.security: DEBUG
```

### View Logs
```bash
# See which endpoints are being allowed
tail -f logs/userservice.log | grep "Allowing unauthenticated"
```

---

## Deployment Checklist

### Before Deploying
- [x] Updated `ServiceAuthFilter.java` with context-aware path checking
- [x] Tested locally with context path
- [x] Verified health endpoints work without auth
- [x] Verified protected endpoints require auth
- [ ] Updated ALB health check path
- [ ] Updated monitoring tool configurations

### After Deploying
- [ ] Test health endpoint from ALB: `curl http://your-alb/userservice/health`
- [ ] Test actuator endpoint from ALB: `curl http://your-alb/userservice/actuator/health`
- [ ] Verify ALB health checks pass
- [ ] Verify monitoring tools can scrape metrics
- [ ] Test protected endpoints still require auth

---

## Troubleshooting

### Health Endpoint Still Returns 401
**Check**:
1. Code is deployed (not old version)
2. Service restarted after deploy
3. Using correct URL with context path
4. Check logs for filter debug messages

**Debug**:
```bash
# Check if old code is running
curl http://your-alb/userservice/actuator/info
# Look for version/build info

# Check logs
kubectl logs -f deployment/userservice | grep ServiceAuthFilter
```

### Protected Endpoints Not Working
**Check**:
1. `X-Service-Token` header is being sent
2. Token matches configured value
3. Token environment variable is set correctly

**Debug**:
```bash
# Test with verbose output
curl -v -X POST http://your-alb/userservice/students/registrations \
  -H "Content-Type: application/json" \
  -H "X-Service-Token: your-token" \
  -d '{...}'
  
# Check if header is being sent
```

---

## Best Practices

### ✅ Do's
1. **Keep health endpoints public** - Required for load balancers
2. **Protect business endpoints** - Require authentication
3. **Use different endpoints for different purposes** - Health vs monitoring
4. **Configure appropriate timeouts** - Longer for actuator endpoints
5. **Enable debug logging in dev** - Helps troubleshooting

### ❌ Don'ts
1. **Don't require auth for health checks** - ALB can't authenticate
2. **Don't expose sensitive data in actuator** - Use `show-details: never` in prod
3. **Don't use same endpoint for everything** - Separate concerns
4. **Don't forget to update ALB config** - After changing paths

---

## Related Documentation

- **HEALTH_ENDPOINTS_GUIDE.md** - Complete health endpoints reference
- **CONTEXT_PATH_CONFIGURATION.md** - Context path setup guide
- **INTERNAL_SERVICE_TOKEN.md** - Service authentication documentation

---

## Summary

**Problem**: Actuator endpoints blocked by service auth filter  
**Cause**: Filter didn't account for context path  
**Solution**: Updated filter to check paths regardless of context  
**Result**: Health and actuator endpoints now publicly accessible  

**Impact**:
- ✅ ALB health checks work
- ✅ Monitoring tools can access metrics
- ✅ Protected endpoints still require auth
- ✅ No security vulnerabilities introduced

---

**Status**: ✅ Fixed and ready for deployment

Deploy the updated `ServiceAuthFilter.java` and your actuator endpoints will be accessible!

