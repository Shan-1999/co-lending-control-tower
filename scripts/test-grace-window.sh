#!/usr/bin/env bash
set -e

YML_PATH="./src/main/resources/application.yml"
BACKUP_PATH="./src/main/resources/application.yml.grace"

echo "=== Grace Window Test ==="

echo "1. Running reconciliation with default 24h grace window..."
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/reconcile" > /dev/null
EVAL_24H=$(curl -s -u operator:operator123 -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_a/groundtruth/ground_truth.json" | jq .phase1Gate)
echo "Outcome with 24h grace window:"
echo "$EVAL_24H"

echo "2. Modifying grace-window-hours from 24 to 2..."
cp "$YML_PATH" "$BACKUP_PATH"
sed -i.tmp 's/grace-window-hours: 24/grace-window-hours: 2/g' "$YML_PATH"
rm -f "$YML_PATH.tmp"

echo "3. Re-running reconciliation..."
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/reconcile" > /dev/null

echo "4. Showing outcome with tightened 2h grace window:"
EVAL_2H=$(curl -s -u operator:operator123 -X GET "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_a/groundtruth/ground_truth.json" | jq .phase1Gate)
echo "Outcome with 2h grace window:"
echo "$EVAL_2H"

echo "5. Restoring application.yml..."
mv "$BACKUP_PATH" "$YML_PATH"
echo "Grace window test complete."
