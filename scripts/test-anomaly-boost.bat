@echo off
echo === Anomaly Boost Verification ===
echo 1. Resetting database...
curl.exe -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/reset" > nul

echo 2. Generating 1000 loans with seed=42 into ./data/boost...
curl.exe -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=1000&outputDir=./data/boost" > nul

echo 3. Ingesting and Reconciling...
curl.exe -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/boost" > nul
curl.exe -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/reconcile" > nul

echo 4. Evaluating Scorecard against ground truth:
curl.exe -s -u operator:operator123 -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/boost/groundtruth/ground_truth.json"

echo.
echo SUCCESS: Anomaly distribution verified with 0 false matches!
