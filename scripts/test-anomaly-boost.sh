#!/usr/bin/env bash
set -e

YML_PATH="./src/main/resources/application.yml"
BACKUP_PATH="./src/main/resources/application.yml.bak"

echo "=== Anomaly Boost Test ==="

echo "1. Saving current application.yml..."
cp "$YML_PATH" "$BACKUP_PATH"

echo "2. Modifying amount-mismatch-rate from 0.02 to 0.10..."
sed -i.tmp 's/amount-mismatch-rate: 0.02/amount-mismatch-rate: 0.10/g' "$YML_PATH"
rm -f "$YML_PATH.tmp"

echo "3. Re-generating with boosted anomaly rate..."
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/reset" > /dev/null
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=1000&outputDir=./data/boost" > /dev/null
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/boost" > /dev/null
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/reconcile" > /dev/null

echo "4. Updated scorecard showing evaluation results:"
curl -s -u operator:operator123 -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/boost/groundtruth/ground_truth.json" | jq .phase1Gate

echo "5. Restoring original application.yml..."
mv "$BACKUP_PATH" "$YML_PATH"
echo "Restoration complete."
