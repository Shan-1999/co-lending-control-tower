#!/usr/bin/env bash
set -e

# ANSI Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}====================================================${NC}"
echo -e "${CYAN}  Co-Lending Control Tower - Initialization Script  ${NC}"
echo -e "${CYAN}====================================================${NC}"

echo -e "\n${YELLOW}[1/8] Checking for Java 21...${NC}"
if ! java -version 2>&1 | grep -q 'version "21'; then
    if ! java -version 2>&1 | grep -q 'openjdk version "21'; then
        echo -e "${RED}Error: Java 21 is required but not found.${NC}"
        java -version
        exit 1
    fi
fi
echo -e "${GREEN}Java 21 found.${NC}"

echo -e "\n${YELLOW}[2/8] Checking PostgreSQL connectivity...${NC}"
PROFILE=""
# Using bash /dev/tcp for cross-platform ping instead of nc which might not be installed
if bash -c '</dev/tcp/localhost/5432' 2>/dev/null; then
    echo -e "${GREEN}PostgreSQL is available on localhost:5432.${NC}"
else
    echo -e "${YELLOW}PostgreSQL unavailable on localhost:5432. Falling back to H2 profile...${NC}"
    PROFILE="-Dspring.profiles.active=h2"
fi

echo -e "\n${YELLOW}[3/8] Running all unit and integration tests...${NC}"
./mvnw clean test
echo -e "${GREEN}Tests passed successfully.${NC}"

echo -e "\n${YELLOW}[4/8] Building and starting the application...${NC}"
./mvnw clean package -DskipTests
java $PROFILE -jar target/co-lending-control-tower-1.0.0.jar &
APP_PID=$!

echo -e "Waiting for application to start up..."
# Wait until the API is up by checking swagger-ui or actuator
RETRIES=0
while ! curl -s -f http://localhost:8080/api-docs > /dev/null; do
    sleep 2
    RETRIES=$((RETRIES+1))
    if [ $RETRIES -gt 30 ]; then
        echo -e "${RED}Application failed to start within 60 seconds.${NC}"
        kill $APP_PID
        exit 1
    fi
    echo -n "."
done
echo -e "\n${GREEN}Application started successfully (PID: $APP_PID)!${NC}"

echo -e "\n${YELLOW}[5/8] Generating 2000 loans with seed=42...${NC}"
curl -s -X POST "http://localhost:8080/api/v1/reset" -u operator:operator123 > /dev/null
curl -s -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=2000&outputDir=./data/seed_a" -u operator:operator123 | jq .

echo -e "\n${YELLOW}[6/8] Running ingestion and reconciliation...${NC}"
echo "Ingesting data..."
curl -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/seed_a" -u operator:operator123 | jq .

echo "Running reconciliation..."
curl -s -X POST "http://localhost:8080/api/v1/reconcile" -u operator:operator123 | jq .

echo -e "\n${YELLOW}[7/8] Running evaluator against ground truth...${NC}"
SCORECARD=$(curl -s -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_a/groundtruth/ground_truth.json" -u operator:operator123)
echo "$SCORECARD" | jq .

echo -e "\n${YELLOW}[8/8] Shutting down application...${NC}"
kill $APP_PID
echo -e "${GREEN}Done!${NC}"
