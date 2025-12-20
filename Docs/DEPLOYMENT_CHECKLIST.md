# Pre-Deployment Checklist - Actuator Fix

## ✅ Code Changes
- [x] ServiceAuthFilter.java updated with context-path aware logic
- [x] Debug logging added
- [x] Comments improved

## ✅ Documentation
- [x] SECURITY_FILTER_FIX.md created
- [x] ACTUATOR_FIX_QUICK_REF.md created
- [x] test-health-endpoints.sh created
- [x] HEALTH_ENDPOINTS_GUIDE.md updated

## ⏳ Before Deploying

### Local Testing
- [ ] Run: `./mvnw spring-boot:run`
- [ ] Test: `curl http://localhost:8080/userservice/actuator/health`
- [ ] Verify: Should return `{"status":"UP"}`
- [ ] Test: `curl http://localhost:8080/userservice/health`
- [ ] Verify: Should return `{"status":"UP","service":"userservice"}`

### Build
- [ ] Run: `./mvnw clean package`
- [ ] Verify: Build successful, no errors
- [ ] Check: `target/userservice-0.0.1-SNAPSHOT.jar` exists

### Docker Build
- [ ] Run: `docker build -t userservice:latest .`
- [ ] Verify: Build successful
- [ ] Optional: Test container locally
  ```bash
  docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev userservice:latest
  curl http://localhost:8080/userservice/actuator/health
  ```

## ⏳ Deployment Steps

### Push to Registry
- [ ] Tag image for your registry
- [ ] Push to ECR/Docker Hub
- [ ] Verify: Image pushed successfully

### Deploy to Environment
- [ ] Deploy to dev/staging first
- [ ] Wait for deployment to complete (~1-2 minutes)
- [ ] Check application logs for startup errors

## ⏳ Post-Deployment Verification

### Health Endpoints (Must Work Without Auth)
```bash
ALB_URL="http://dev-microservices-alb-2015993104.ap-south-1.elb.amazonaws.com"
```

- [ ] Test: `curl $ALB_URL/userservice/health`
  - Expected: `{"status":"UP","service":"userservice"}`
  
- [ ] Test: `curl $ALB_URL/userservice/actuator/health`
  - Expected: `{"status":"UP"}`
  - NOT: `{"code":"UNAUTHORIZED",...}`

- [ ] Test: `curl $ALB_URL/userservice/actuator/info`
  - Expected: Application info JSON

- [ ] Test: `curl $ALB_URL/userservice/actuator/metrics`
  - Expected: Metrics list

### Protected Endpoints (Must Still Require Auth)
- [ ] Test without token: `curl -X POST $ALB_URL/userservice/students/registrations`
  - Expected: `{"code":"UNAUTHORIZED",...}`

- [ ] Test with token: `curl -H "X-Service-Token: your-token" $ALB_URL/userservice/students/registrations`
  - Expected: Different error (not UNAUTHORIZED)

### ALB Health Check
- [ ] Check AWS Console → Target Groups → userservice
- [ ] Verify: All targets showing "healthy"
- [ ] Check: No intermittent unhealthy targets

### Application Logs
- [ ] Check logs for errors
- [ ] Look for: `"Allowing unauthenticated access to health/actuator endpoint"`
- [ ] Verify: No authentication errors for health endpoints

### Run Test Script
- [ ] Run: `./test-health-endpoints.sh $ALB_URL`
- [ ] Verify: All health/actuator tests pass (tests 1-4)
- [ ] Verify: Protected endpoint test fails with 401 (test 5)

## ⏳ Monitoring Setup

### Prometheus (if using)
- [ ] Update scrape config with `/userservice/actuator/prometheus`
- [ ] Verify: Metrics being scraped
- [ ] Check: No authentication errors in Prometheus logs

### CloudWatch (if using)
- [ ] Verify: ALB health check metrics showing healthy
- [ ] Check: No 401 errors in access logs for `/actuator/*`

### Grafana (if using)
- [ ] Test dashboard queries
- [ ] Verify: Metrics displaying correctly

## ⏳ If Something Goes Wrong

### Rollback Plan
- [ ] Keep previous version image ready
- [ ] Know how to quickly rollback deployment
- [ ] Have old task definition/deployment ready

### Debug Steps
1. [ ] Check if new code is actually deployed
   ```bash
   curl $ALB_URL/userservice/actuator/info | jq '.build'
   ```

2. [ ] Check application logs
   ```bash
   # ECS
   aws logs tail /ecs/userservice --follow
   
   # K8s
   kubectl logs -f deployment/userservice
   ```

3. [ ] Test locally to isolate issue
   ```bash
   ./mvnw spring-boot:run
   curl http://localhost:8080/userservice/actuator/health
   ```

4. [ ] Verify environment variables
   ```bash
   # Check INTERNAL_SERVICE_TOKEN is set
   kubectl describe deployment userservice | grep INTERNAL_SERVICE_TOKEN
   ```

## ✅ Success Criteria

All of these must be true:
- [x] Code compiled without errors
- [ ] Local testing passed
- [ ] Docker build successful
- [ ] Deployment completed without errors
- [ ] Health endpoint returns 200
- [ ] Actuator health returns 200
- [ ] Protected endpoints still require auth
- [ ] ALB health checks passing
- [ ] No authentication errors in logs
- [ ] Monitoring/metrics working

## 📝 Sign-Off

- [ ] Tested by: _________________
- [ ] Date: _________________
- [ ] Environment: Dev / Staging / Production
- [ ] Status: ✅ Pass / ❌ Fail
- [ ] Notes: _________________

---

**When all checkboxes are complete, the fix is successfully deployed!**

Use this checklist to ensure nothing is missed during deployment.

