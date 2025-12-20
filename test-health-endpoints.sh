#!/bin/bash

# Verification Script for Actuator Health Endpoints
# Tests that health/actuator endpoints work without authentication

echo "============================================"
echo "Testing Health Endpoints (No Auth Required)"
echo "============================================"

# Configuration
BASE_URL="${1:-http://localhost:8080}"
CONTEXT_PATH="/userservice"

echo ""
echo "Testing against: ${BASE_URL}${CONTEXT_PATH}"
echo ""

# Test 1: Custom Health Endpoint
echo "1. Testing custom health endpoint..."
RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}${CONTEXT_PATH}/health")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ PASS: Custom health endpoint accessible"
    echo "   Response: $BODY"
else
    echo "❌ FAIL: Custom health endpoint returned $HTTP_CODE"
    echo "   Response: $BODY"
fi

echo ""

# Test 2: Actuator Health Endpoint
echo "2. Testing actuator health endpoint..."
RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}${CONTEXT_PATH}/actuator/health")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ PASS: Actuator health endpoint accessible"
    echo "   Response: $BODY"
else
    echo "❌ FAIL: Actuator health endpoint returned $HTTP_CODE"
    echo "   Response: $BODY"
fi

echo ""

# Test 3: Actuator Info Endpoint
echo "3. Testing actuator info endpoint..."
RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}${CONTEXT_PATH}/actuator/info")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ PASS: Actuator info endpoint accessible"
    echo "   Response: $BODY"
else
    echo "❌ FAIL: Actuator info endpoint returned $HTTP_CODE"
    echo "   Response: $BODY"
fi

echo ""

# Test 4: Actuator Metrics Endpoint
echo "4. Testing actuator metrics endpoint..."
RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}${CONTEXT_PATH}/actuator/metrics")
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ PASS: Actuator metrics endpoint accessible"
    # Just show first 200 chars to avoid spam
    echo "   Response: $(echo $BODY | cut -c1-200)..."
else
    echo "❌ FAIL: Actuator metrics endpoint returned $HTTP_CODE"
    echo "   Response: $BODY"
fi

echo ""

# Test 5: Protected Endpoint Without Auth (Should Fail)
echo "5. Testing protected endpoint without auth (should fail)..."
RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}${CONTEXT_PATH}/students/registrations" \
    -X POST \
    -H "Content-Type: application/json" \
    -d '{"fullName":"Test User","email":"test@example.com"}')
HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" -eq 401 ]; then
    echo "✅ PASS: Protected endpoint correctly requires authentication"
    echo "   Response: $BODY"
else
    echo "⚠️  WARNING: Protected endpoint returned $HTTP_CODE (expected 401)"
    echo "   Response: $BODY"
fi

echo ""
echo "============================================"
echo "Test Summary"
echo "============================================"
echo ""
echo "Expected Results:"
echo "  1-4: All should return 200 (publicly accessible)"
echo "  5:   Should return 401 (protected)"
echo ""
echo "If any health/actuator tests failed, the ServiceAuthFilter"
echo "may not have been updated to handle the context path."
echo ""
echo "Usage: $0 [base-url]"
echo "Example: $0 http://dev-microservices-alb-2015993104.ap-south-1.elb.amazonaws.com"

