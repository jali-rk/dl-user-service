#!/bin/bash
# Test Runner Script with Detailed Output
# Run this to see exactly what's happening with your tests

echo "============================================"
echo "USER SERVICE TEST RUNNER"
echo "============================================"
echo ""

# Check Java version
echo "1. Checking Java version..."
java -version
echo ""

# Check Maven
echo "2. Checking Maven..."
./mvnw --version
echo ""

# Clean and compile
echo "3. Cleaning and compiling..."
./mvnw clean compile -q
if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
else
    echo "❌ Compilation failed"
    exit 1
fi
echo ""

# Run Repository Tests (Fastest, most isolated)
echo "4. Running Repository Tests..."
./mvnw test -Dtest=UserRepositoryTest -q
if [ $? -eq 0 ]; then
    echo "✅ Repository tests passed"
else
    echo "❌ Repository tests failed"
fi
echo ""

# Run Unit Tests
echo "5. Running Unit Tests (Service Layer)..."
./mvnw test -Dtest=UserServiceImplTest -q
if [ $? -eq 0 ]; then
    echo "✅ Unit tests passed"
else
    echo "❌ Unit tests failed"
fi
echo ""

# Run Integration Tests
echo "6. Running Integration Tests..."
./mvnw test -Dtest="*IntegrationTest" -q
if [ $? -eq 0 ]; then
    echo "✅ Integration tests passed"
else
    echo "❌ Integration tests failed"
fi
echo ""

# Run all tests
echo "7. Running ALL tests..."
./mvnw test
echo ""

echo "============================================"
echo "TEST EXECUTION COMPLETE"
echo "============================================"
echo ""
echo "Check target/surefire-reports/ for detailed reports"
echo "Run './mvnw test -X' for debug output"

