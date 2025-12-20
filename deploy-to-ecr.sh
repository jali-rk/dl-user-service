#!/bin/bash
# Manual deployment script for userservice to ECR and ECS
# Use this if GitHub Actions workflow is not working

set -e

echo "🚀 Deploying userservice to ECR and ECS..."
echo ""

# Configuration - UPDATE THESE VALUES
AWS_REGION="ap-south-1"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-YOUR_ACCOUNT_ID_HERE}"  # Set via environment or update here
ECR_REPOSITORY="userservice"
ECS_CLUSTER="${ECS_CLUSTER:-your-cluster-name}"  # Set via environment or update here
ECS_SERVICE="userservice"
ALB_URL="http://dev-microservices-alb-2015993104.ap-south-1.elb.amazonaws.com"

echo "📋 Configuration:"
echo "  Region: $AWS_REGION"
echo "  Account: $AWS_ACCOUNT_ID"
echo "  ECR Repo: $ECR_REPOSITORY"
echo "  ECS Cluster: $ECS_CLUSTER"
echo "  ECS Service: $ECS_SERVICE"
echo "  ALB URL: $ALB_URL"
echo ""

# Validation
if [ "$AWS_ACCOUNT_ID" = "YOUR_ACCOUNT_ID_HERE" ]; then
    echo "❌ ERROR: Please set AWS_ACCOUNT_ID environment variable or update the script"
    echo "   Example: export AWS_ACCOUNT_ID=123456789012"
    exit 1
fi

if [ "$ECS_CLUSTER" = "your-cluster-name" ]; then
    echo "❌ ERROR: Please set ECS_CLUSTER environment variable or update the script"
    echo "   Example: export ECS_CLUSTER=my-cluster"
    exit 1
fi

# Check if Docker image exists
if ! docker images userservice:latest | grep -q userservice; then
    echo "❌ ERROR: Docker image 'userservice:latest' not found"
    echo "   Run: ./mvnw clean package && docker build -t userservice:latest ."
    exit 1
fi

echo "✅ Docker image found"
echo ""

# Login to ECR
echo "🔐 Logging in to ECR..."
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

if [ $? -ne 0 ]; then
    echo "❌ ERROR: Failed to login to ECR"
    echo "   Check your AWS credentials and permissions"
    exit 1
fi

echo "✅ ECR login successful"
echo ""

# Tag image
echo "🏷️  Tagging image..."
ECR_IMAGE="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY:latest"
docker tag userservice:latest $ECR_IMAGE

echo "✅ Image tagged: $ECR_IMAGE"
echo ""

# Push to ECR
echo "⬆️  Pushing to ECR (this may take a minute)..."
docker push $ECR_IMAGE

if [ $? -ne 0 ]; then
    echo "❌ ERROR: Failed to push to ECR"
    exit 1
fi

echo "✅ Image pushed to ECR"
echo ""

# Get current task definition
echo "📝 Getting current task definition..."
TASK_FAMILY=$(aws ecs describe-services \
  --cluster $ECS_CLUSTER \
  --services $ECS_SERVICE \
  --region $AWS_REGION \
  --query 'services[0].taskDefinition' \
  --output text | cut -d'/' -f2 | cut -d':' -f1)

echo "   Task family: $TASK_FAMILY"
echo ""

# Force deployment
echo "🔄 Forcing ECS deployment..."
aws ecs update-service \
  --cluster $ECS_CLUSTER \
  --service $ECS_SERVICE \
  --force-new-deployment \
  --region $AWS_REGION \
  --no-cli-pager

if [ $? -ne 0 ]; then
    echo "❌ ERROR: Failed to update ECS service"
    exit 1
fi

echo "✅ ECS deployment started"
echo ""

# Wait for deployment
echo "⏳ Waiting for deployment to stabilize (this may take 2-3 minutes)..."
echo "   You can check progress in AWS Console: ECS → Clusters → $ECS_CLUSTER → Services → $ECS_SERVICE"
echo ""

aws ecs wait services-stable \
  --cluster $ECS_CLUSTER \
  --services $ECS_SERVICE \
  --region $AWS_REGION

if [ $? -ne 0 ]; then
    echo "⚠️  WARNING: Deployment may have issues"
    echo "   Check ECS console for details"
else
    echo "✅ Deployment stabilized"
fi

echo ""
echo "🧪 Testing endpoints..."
echo ""

# Test health endpoint
echo "1. Testing custom health endpoint..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" $ALB_URL/userservice/health)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n 1)
BODY=$(echo "$HEALTH_RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Custom health: $BODY"
else
    echo "   ❌ Custom health returned $HTTP_CODE: $BODY"
fi

echo ""

# Test actuator health endpoint
echo "2. Testing actuator health endpoint..."
ACTUATOR_RESPONSE=$(curl -s -w "\n%{http_code}" $ALB_URL/userservice/actuator/health)
HTTP_CODE=$(echo "$ACTUATOR_RESPONSE" | tail -n 1)
BODY=$(echo "$ACTUATOR_RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Actuator health: $BODY"
else
    echo "   ❌ Actuator health returned $HTTP_CODE: $BODY"
    echo "   ⚠️  If you see UNAUTHORIZED, wait a few more seconds and try again"
fi

echo ""

# Test actuator metrics
echo "3. Testing actuator metrics endpoint..."
METRICS_RESPONSE=$(curl -s -w "\n%{http_code}" $ALB_URL/userservice/actuator/metrics)
HTTP_CODE=$(echo "$METRICS_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Actuator metrics: Accessible"
else
    BODY=$(echo "$METRICS_RESPONSE" | head -n -1)
    echo "   ❌ Actuator metrics returned $HTTP_CODE: $BODY"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎉 Deployment Complete!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔗 Endpoints:"
echo "   Health:    $ALB_URL/userservice/health"
echo "   Actuator:  $ALB_URL/userservice/actuator/health"
echo "   Metrics:   $ALB_URL/userservice/actuator/metrics"
echo "   Info:      $ALB_URL/userservice/actuator/info"
echo ""
echo "If actuator endpoints still return UNAUTHORIZED, wait 30 seconds"
echo "for all ECS tasks to be replaced with new ones, then test again."
echo ""

