#!/bin/bash
# Monitor deployment status

ALB_URL="http://dev-microservices-alb-2015993104.ap-south-1.elb.amazonaws.com"

echo "🔍 Monitoring Actuator Health Endpoint Deployment"
echo "=================================================="
echo ""
echo "Workflow URL: https://github.com/jali-rk/dl-user-service/actions"
echo "Testing: $ALB_URL/userservice/actuator/health"
echo ""
echo "Checking every 30 seconds..."
echo "Press Ctrl+C to stop"
echo ""

ATTEMPT=1

while true; do
    echo "[$ATTEMPT] $(date '+%H:%M:%S') - Testing..."

    RESPONSE=$(curl -s -w "\n%{http_code}" $ALB_URL/userservice/actuator/health 2>/dev/null)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | head -n -1)

    if [ "$HTTP_CODE" = "200" ]; then
        if echo "$BODY" | grep -q '"status":"UP"'; then
            echo "    ✅ SUCCESS! Actuator health is working!"
            echo "    Response: $BODY"
            echo ""
            echo "🎉 Deployment successful! All actuator endpoints should now work."
            echo ""
            echo "Test other endpoints:"
            echo "  curl $ALB_URL/userservice/health"
            echo "  curl $ALB_URL/userservice/actuator/metrics"
            echo "  curl $ALB_URL/userservice/actuator/info"
            exit 0
        else
            echo "    ⚠️  Got 200 but unexpected response: $BODY"
        fi
    else
        echo "    ❌ HTTP $HTTP_CODE: $BODY"
        echo "    (Still deploying...)"
    fi

    ATTEMPT=$((ATTEMPT + 1))
    sleep 30
done

