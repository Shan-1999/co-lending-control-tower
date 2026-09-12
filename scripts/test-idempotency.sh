#!/usr/bin/env bash
set -e

echo "=== Idempotency Test ==="

echo "1. Resetting DB and generating 500 loans..."
curl -s -X POST "http://localhost:8080/api/v1/reset" -u operator:operator123 > /dev/null
curl -s -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=500&outputDir=./data/idempotency" -u operator:operator123 > /dev/null

echo "2. First Ingestion..."
FIRST_INGEST=$(curl -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/idempotency" -u operator:operator123)
PROCESSED_1=$(echo "$FIRST_INGEST" | jq .processedRecords)
SKIPPED_1=$(echo "$FIRST_INGEST" | jq .skippedDuplicates)
TOTAL_1=$(echo "$FIRST_INGEST" | jq .totalRecords)

echo "First ingestion: total=$TOTAL_1, processed=$PROCESSED_1, skipped=$SKIPPED_1"

echo "3. Second Ingestion (exact same data directory)..."
SECOND_INGEST=$(curl -s -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/idempotency" -u operator:operator123)
PROCESSED_2=$(echo "$SECOND_INGEST" | jq .processedRecords)
SKIPPED_2=$(echo "$SECOND_INGEST" | jq .skippedDuplicates)

echo "Second ingestion: processed=$PROCESSED_2, skipped=$SKIPPED_2"

echo "4. Asserting 0 new mutations and 100% duplicate skip..."
if [ "$PROCESSED_2" -eq 0 ] && [ "$SKIPPED_2" -eq "$TOTAL_1" ]; then
    echo "SUCCESS: 0 new records inserted, all $SKIPPED_2 duplicates skipped. Ingestion is 100% idempotent!"
else
    echo "ERROR: Idempotency check failed! Processed: $PROCESSED_2, Expected 0."
    exit 1
fi
