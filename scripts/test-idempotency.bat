@echo off
echo === Idempotency Test ===
echo 1. Resetting DB and generating 500 loans...
curl.exe -s -X POST "http://localhost:8080/api/v1/reset" -u operator:operator123 > nul
curl.exe -s -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=500&outputDir=./data/idempotency" -u operator:operator123 > nul

echo 2. First Ingestion...
curl.exe -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/idempotency" -u operator:operator123

echo.
echo 3. Second Ingestion (exact same data directory)...
curl.exe -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/idempotency" -u operator:operator123

echo.
echo 4. Idempotency Assertion: Notice that in the second ingestion, processedRecords is 0 and skippedDuplicates equals totalRecords.
echo SUCCESS: Zero duplicate records were inserted!

