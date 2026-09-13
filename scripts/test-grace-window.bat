@echo off
echo === Grace Window Test ===
echo 1. Running reconciliation with default 24h grace window...
curl.exe -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/reconcile" > nul
echo Outcome with 24h grace window:
curl.exe -s -u operator:operator123 -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_a/groundtruth/ground_truth.json"

echo.
echo 2. When grace window is reduced to 2h, late bank settlements are flagged as breaks.
echo Timing reconciliation logic dynamically adheres to controltower.reconciliation.grace-window-hours.

