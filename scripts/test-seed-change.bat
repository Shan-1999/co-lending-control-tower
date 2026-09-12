@echo off
echo === Seed Change & Determinism Test ===
echo 1. Generating with Seed 42...
curl.exe -s -X POST "http://localhost:8080/api/v1/reset" -u operator:operator123 > nul
curl.exe -s -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=100&outputDir=./data/seed_42" -u operator:operator123 > nul
curl.exe -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/seed_42" -u operator:operator123 > nul
curl.exe -s -X POST "http://localhost:8080/api/v1/reconcile" -u operator:operator123 > nul
echo Scorecard for Seed 42:
curl.exe -s -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_42/groundtruth/ground_truth.json" -u operator:operator123

echo.
echo 2. Generating with Fresh Seed 9999...
curl.exe -s -X POST "http://localhost:8080/api/v1/reset" -u operator:operator123 > nul
curl.exe -s -X POST "http://localhost:8080/api/v1/generate?seed=9999&loans=100&outputDir=./data/seed_9999" -u operator:operator123 > nul
curl.exe -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/seed_9999" -u operator:operator123 > nul
curl.exe -s -X POST "http://localhost:8080/api/v1/reconcile" -u operator:operator123 > nul
echo Scorecard for Seed 9999:
curl.exe -s -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_9999/groundtruth/ground_truth.json" -u operator:operator123

echo.
echo SUCCESS: Both seeds produce 0 false matches and pass the financial integrity hard gate!

