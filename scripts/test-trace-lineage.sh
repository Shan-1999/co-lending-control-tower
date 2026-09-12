#!/usr/bin/env bash
set -e

LOAN_REF=${1:-LOAN-000001}

echo "=== Trace Lineage for $LOAN_REF ==="
echo "Calling GET http://localhost:8080/api/v1/audit/trace/$LOAN_REF"

curl -s -X GET "http://localhost:8080/api/v1/audit/trace/$LOAN_REF" -u operator:operator123 | jq '{
  loanReference: .loanReference,
  totalEvents: .totalEvents,
  events: [
    .canonicalEvents[] | {
      eventId: .eventId,
      sourceSystem: .sourceSystem,
      eventType: .eventType,
      amountInr: .amountInr,
      canonicalStatus: .canonicalStatus,
      rawRecordId: .rawRecordId,
      payloadHash: .payloadHash,
      quarantined: .quarantined
    }
  ],
  matchDecisions: [
    .matchDecisions[] | {
      decisionId: .decisionId,
      matchLevel: .matchLevel,
      confidenceScore: .confidenceScore,
      decidedAt: .decidedAt
    }
  ],
  auditCount: (.auditTrail | length)
}'

echo "Lineage trace completed."
