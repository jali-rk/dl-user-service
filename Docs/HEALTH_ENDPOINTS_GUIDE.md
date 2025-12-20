# Health Check Endpoints - Complete Guide

## Overview

With the context path `/userservice` configured, you now have **two health check endpoints** for different purposes:

1. **Custom Health Endpoint**: `/userservice/health` - Simple, fast, for ALB
2. **Actuator Health Endpoint**: `/userservice/actuator/health` - Detailed, for monitoring

---

## 📍 Endpoint Locations

### 1. Custom Health Endpoint (Recommended for ALB)

**Path**: `/userservice/health`  
**Full URL**: `http://localhost:8080/userservice/health`  
**Purpose**: Simple, fast health check optimized for load balancers  
**Authentication**: None required  
**Response Time**: ~5-10ms  

**Response**:
```json
{
  "status": "UP",
  "service": "userservice"
}
```

**Use Cases**:
- ✅ ALB/ELB health checks
- ✅ Quick availability check
- ✅ External monitoring tools
- ✅ Simple uptime monitoring

---

### 2. Actuator Health Endpoint (For Monitoring)

**Path**: `/userservice/actuator/health`  
**Full URL**: `http://localhost:8080/userservice/actuator/health`  
**Purpose**: Detailed health information with component breakdown  
**Authentication**: Optional (configured for detailed view)  
**Response Time**: ~20-50ms (checks all components)

**Response (Basic)**:
```json
{
  "status": "UP"
}
```

**Response (Detailed - when authorized)**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Use Cases**:
- ✅ Application performance monitoring (APM)
- ✅ Detailed health diagnostics
- ✅ Component-level health checks
- ✅ Production troubleshooting

---

## 🎯 Which Endpoint Should You Use?

| Scenario | Recommended Endpoint | Reason |
|----------|---------------------|---------|
| **ALB Health Check** | `/userservice/health` | Fast, simple, minimal overhead |
| **Kubernetes Liveness** | `/userservice/health` | Quick check, low resource usage |
| **Kubernetes Readiness** | `/userservice/actuator/health` | Checks DB and dependencies |
| **Monitoring Dashboard** | `/userservice/actuator/health` | Detailed status information |
| **CI/CD Health Check** | `/userservice/health` | Fast validation after deployment |
| **Production Debugging** | `/userservice/actuator/health` | Component-level diagnostics |

---

## 🔧 Configuration Details

### Custom Health Endpoint
**Controller**: `HealthController.java`
```java
@GetMapping("/health")
public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "userservice"
    ));
}
```

**Benefits**:
- No dependency checks
- Instant response
- Simple JSON structure
- Perfect for high-frequency health checks

---

### Actuator Health Endpoint

#### Development Configuration (`application-dev.yml`)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized  # Show details in dev
  health:
    defaults:
      enabled: true
```

#### Production Configuration (`application-prod.yml`)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: never  # Hide details in prod for security
  health:
    defaults:
      enabled: true
```

**Features**:
- Database connectivity check
- Disk space check
- Custom health indicators
- Metrics integration
- Full Spring Boot Actuator features

---

## 🏗️ ALB Configuration

### Option 1: Use Custom Health Endpoint (Recommended)

```hcl
# Terraform - ALB Target Group
resource "aws_lb_target_group" "userservice" {
  name     = "userservice-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = var.vpc_id
  
  health_check {
    enabled             = true
    path                = "/userservice/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    matcher             = "200"
  }
}
```

**Why use `/userservice/health`?**
- ✅ Faster response time
- ✅ Lower resource usage
- ✅ No database dependency
- ✅ Simpler to debug

---

### Option 2: Use Actuator Health Endpoint

```hcl
resource "aws_lb_target_group" "userservice" {
  name     = "userservice-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = var.vpc_id
  
  health_check {
    enabled             = true
    path                = "/userservice/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10  # Longer timeout for dependency checks
    interval            = 30
    matcher             = "200"
  }
}
```

**Why use `/userservice/actuator/health`?**
- ✅ Validates database connectivity
- ✅ Checks all dependencies
- ✅ More comprehensive health check
- ⚠️ Slightly slower response

---

## 🐳 Docker Configuration

### Health Check in Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app
COPY target/userservice-*.jar app.jar

# Use custom health endpoint for container health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/userservice/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Health Check in Docker Compose

```yaml
services:
  userservice:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/userservice/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 40s
```

---

## ☸️ Kubernetes Configuration

### Liveness Probe (Use Custom Health)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: userservice
spec:
  template:
    spec:
      containers:
      - name: userservice
        image: userservice:latest
        ports:
        - containerPort: 8080
        
        # Liveness: Is the app running?
        livenessProbe:
          httpGet:
            path: /userservice/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # Readiness: Can the app handle traffic?
        readinessProbe:
          httpGet:
            path: /userservice/actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 10
          failureThreshold: 3
```

**Why different probes?**
- **Liveness**: Fast check to restart if frozen
- **Readiness**: Comprehensive check including DB before routing traffic

---

## 📊 Other Actuator Endpoints

With the current configuration, these endpoints are also available:

### 1. Info Endpoint
**Path**: `/userservice/actuator/info`  
**Purpose**: Application metadata, version, build info

**Response**:
```json
{
  "app": {
    "name": "userservice",
    "description": "User Service for DopamineLite Platform",
    "version": "0.0.1-SNAPSHOT"
  }
}
```

### 2. Metrics Endpoint
**Path**: `/userservice/actuator/metrics`  
**Purpose**: Application metrics (JVM, HTTP, custom)

**Response**:
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "http.server.requests",
    "system.cpu.usage",
    "process.uptime"
  ]
}
```

**Specific Metric**: `/userservice/actuator/metrics/http.server.requests`

### 3. Prometheus Endpoint
**Path**: `/userservice/actuator/prometheus`  
**Purpose**: Metrics in Prometheus format for scraping

---

## 🔐 Security Considerations

### Authentication Requirements

**Health and Actuator endpoints are PUBLIC** - No `X-Service-Token` required.

The `ServiceAuthFilter` allows unauthenticated access to:
- Any path ending with `/health`
- Any path containing `/actuator/`
- Any path ending with `/actuator`

**Why it's safe**:
- Health endpoints are designed to be public
- Required for ALB health checks and monitoring
- Production config hides sensitive details (`show-details: never`)
- Standard practice for microservices

**Protected endpoints** (require `X-Service-Token`):
- `/userservice/students/*`
- `/userservice/admins/*`
- `/userservice/users/*`
- `/userservice/auth/*`
- `/userservice/internal/auth/*`

See `SECURITY_FILTER_FIX.md` for implementation details.

### Production Best Practices

1. **Hide Actuator Details**
```yaml
management:
  endpoint:
    health:
      show-details: never  # Don't expose internal details
```

2. **Limit Exposed Endpoints**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # Only essential endpoints
```

3. **Use Separate Management Port** (Optional)
```yaml
management:
  server:
    port: 9090  # Actuator on different port
  endpoints:
    web:
      base-path: /actuator
```

Then configure firewall to block port 9090 from external access.

4. **Add Authentication** (Advanced)
```yaml
# Only allow authenticated access to actuator
spring:
  security:
    user:
      name: admin
      password: ${ACTUATOR_PASSWORD}
```

---

## 🧪 Testing the Endpoints

### Test Custom Health Endpoint
```bash
# Start service
./mvnw spring-boot:run

# Test custom health
curl http://localhost:8080/userservice/health

# Expected response:
# {"status":"UP","service":"userservice"}
```

### Test Actuator Health Endpoint
```bash
# Basic health check
curl http://localhost:8080/userservice/actuator/health

# Expected response (prod):
# {"status":"UP"}

# Expected response (dev with details):
# {"status":"UP","components":{...}}
```

### Test Other Actuator Endpoints
```bash
# Info endpoint
curl http://localhost:8080/userservice/actuator/info

# Metrics list
curl http://localhost:8080/userservice/actuator/metrics

# Specific metric
curl http://localhost:8080/userservice/actuator/metrics/jvm.memory.used

# Prometheus format
curl http://localhost:8080/userservice/actuator/prometheus
```

---

## 📈 Monitoring Integration Examples

### Prometheus Scrape Configuration
```yaml
scrape_configs:
  - job_name: 'userservice'
    metrics_path: '/userservice/actuator/prometheus'
    static_configs:
      - targets: ['userservice:8080']
```

### Grafana Dashboard Query
```promql
# HTTP request rate
rate(http_server_requests_seconds_count{application="userservice"}[5m])

# JVM memory usage
jvm_memory_used_bytes{application="userservice",area="heap"}

# DB connection pool
hikaricp_connections_active{application="userservice"}
```

### CloudWatch Custom Metrics (AWS)
```bash
# Push custom metrics from actuator
aws cloudwatch put-metric-data \
  --namespace UserService \
  --metric-name HealthStatus \
  --value 1 \
  --dimensions Service=userservice
```

---

## 🔄 Migration from Old Endpoints

### If You Were Using `/actuator/health` Without Context Path

**Before**:
```
http://localhost:8080/actuator/health
```

**After**:
```
http://localhost:8080/userservice/actuator/health
```

### Update Required In

1. **ALB Target Groups** - Update health check path
2. **Kubernetes Manifests** - Update probe paths
3. **Docker Compose** - Update healthcheck commands
4. **Monitoring Tools** - Update scrape endpoints
5. **Documentation** - Update API docs

---

## 💡 Recommendations

### For ALB Health Checks
✅ **Use**: `/userservice/health`
- Fast
- Simple
- Low overhead
- Perfect for high-frequency checks

### For Application Monitoring
✅ **Use**: `/userservice/actuator/health`
- Comprehensive
- Component breakdown
- Dependency validation
- Rich metrics

### For CI/CD Pipelines
✅ **Use**: `/userservice/health`
- Quick validation
- Fast feedback
- No external dependencies

### For Production Debugging
✅ **Use**: `/userservice/actuator/health` with auth
- Detailed diagnostics
- Component status
- Real-time health data

---

## 📋 Summary

| Aspect | Custom Health | Actuator Health |
|--------|--------------|----------------|
| **Path** | `/userservice/health` | `/userservice/actuator/health` |
| **Speed** | Very fast (~5ms) | Moderate (~30ms) |
| **Dependencies** | None | Checks DB, disk, etc. |
| **Use Case** | ALB, quick checks | Monitoring, debugging |
| **Response** | Simple JSON | Detailed components |
| **Configuration** | Controller | application.yml |
| **Auth Required** | No | Optional |
| **Best For** | High-frequency checks | Comprehensive monitoring |

---

## ✅ Quick Reference

```bash
# Custom health (for ALB)
curl http://localhost:8080/userservice/health

# Actuator health (for monitoring)
curl http://localhost:8080/userservice/actuator/health

# Application info
curl http://localhost:8080/userservice/actuator/info

# Metrics
curl http://localhost:8080/userservice/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/userservice/actuator/prometheus
```

---

**✨ Both endpoints work perfectly with the `/userservice` context path!**

Choose the right endpoint for the right purpose:
- **ALB/Load Balancers** → `/userservice/health`
- **Monitoring/Debugging** → `/userservice/actuator/health`

