# Health Endpoints - Quick Reference Card

## 🎯 Two Endpoints, Two Purposes

```
Custom Health:   /userservice/health              ← Use for ALB
Actuator Health: /userservice/actuator/health     ← Use for monitoring
```

---

## ⚡ Quick Comparison

| | Custom | Actuator |
|---|---|---|
| **Speed** | 5ms | 30ms |
| **Checks** | None | DB, disk, etc. |
| **Best For** | ALB | Monitoring |
| **ALB Recommended** | ✅ Yes | ⚠️ Works but slower |

---

## 🔧 ALB Configuration

```hcl
# Recommended
health_check {
  path = "/userservice/health"
  timeout = 5
}
```

---

## 🧪 Test Commands

```bash
# Custom (fast)
curl http://localhost:8080/userservice/health

# Actuator (detailed)
curl http://localhost:8080/userservice/actuator/health

# Metrics
curl http://localhost:8080/userservice/actuator/metrics

# Prometheus
curl http://localhost:8080/userservice/actuator/prometheus
```

---

## 📊 Available Actuator Endpoints

```
/userservice/actuator/health      ← Health status
/userservice/actuator/info        ← App info
/userservice/actuator/metrics     ← Metrics list
/userservice/actuator/prometheus  ← Prometheus format
```

---

## 💡 Recommendation

**ALB**: Use `/userservice/health` (faster, more reliable)  
**Monitoring**: Use `/userservice/actuator/health` (detailed)

---

See `HEALTH_ENDPOINTS_GUIDE.md` for complete documentation.

