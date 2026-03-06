#!/usr/bin/env bash
# submit-responses.sh — Generate and submit dummy responses to questions.
#
# Usage:
#   ./submit-responses.sh DEVICE_ID QUESTION_ID [ANSWER] [BASE_URL]
#
# Required Parameters:
#   DEVICE_ID    UUID of a paired device (obtain from add-devices.sh output)
#   QUESTION_ID  UUID of an existing question
#
# Optional Parameters:
#   ANSWER       The answer string (default: "Sample answer from device")
#                For WRITTEN_ANSWER questions: any text string
#                For MULTIPLE_CHOICE questions: an integer index (e.g. "0", "1", "2")
#   BASE_URL     Server base URL (default: http://localhost:5911)
#
# Environment Variables (for advanced customisation):
#   RESPONSE_ID     Override the auto-generated response UUID
#   TIMESTAMP       Override the auto-generated ISO 8601 timestamp
#   IS_CORRECT      Set to "true" or "false" to include correctness evaluation
#   RESPONSE_COUNT  Number of responses to submit (default: 1, each with a unique ID)
#
# Examples:
#   # Submit a written answer
#   ./submit-responses.sh \
#     "550e8400-e29b-41d4-a716-446655440000" \
#     "660e8400-e29b-41d4-a716-446655440001" \
#     "The answer is photosynthesis"
#
#   # Submit a multiple-choice answer (select option index 2)
#   ./submit-responses.sh \
#     "550e8400-e29b-41d4-a716-446655440000" \
#     "660e8400-e29b-41d4-a716-446655440001" \
#     "2"
#
#   # Submit 5 responses with different random IDs
#   RESPONSE_COUNT=5 ./submit-responses.sh \
#     "550e8400-e29b-41d4-a716-446655440000" \
#     "660e8400-e29b-41d4-a716-446655440001" \
#     "My answer"
#
#   # Submit with correctness flag
#   IS_CORRECT=true ./submit-responses.sh \
#     "550e8400-e29b-41d4-a716-446655440000" \
#     "660e8400-e29b-41d4-a716-446655440001" \
#     "1"
#
# The server must be running. The device must be paired (use add-devices.sh)
# and the question must exist (create via the UI or another script).
#
# Per API Contract §2.3, responses are submitted via POST /api/v1/responses.
# The response body conforms to Validation Rules §2C.

set -euo pipefail

DEVICE_ID="${1:?Usage: $0 DEVICE_ID QUESTION_ID [ANSWER] [BASE_URL]}"
QUESTION_ID="${2:?Usage: $0 DEVICE_ID QUESTION_ID [ANSWER] [BASE_URL]}"
ANSWER="${3:-Sample answer from device}"
BASE_URL="${4:-http://localhost:5911}"
RESPONSE_COUNT="${RESPONSE_COUNT:-1}"

generate_uuid() {
  python3 -c "import uuid; print(uuid.uuid4())"
}

generate_timestamp() {
  python3 -c "from datetime import datetime, timezone; print(datetime.now(timezone.utc).isoformat())"
}

submit_one() {
  local rid="${RESPONSE_ID:-$(generate_uuid)}"
  local ts="${TIMESTAMP:-$(generate_timestamp)}"

  # Build JSON payload
  local json
  if [ -n "${IS_CORRECT:-}" ]; then
    json=$(python3 -c "
import json, sys
print(json.dumps({
    'Id': '$rid',
    'QuestionId': '$QUESTION_ID',
    'DeviceId': '$DEVICE_ID',
    'Answer': '$ANSWER',
    'Timestamp': '$ts',
    'IsCorrect': $IS_CORRECT
}))
")
  else
    json=$(python3 -c "
import json, sys
print(json.dumps({
    'Id': '$rid',
    'QuestionId': '$QUESTION_ID',
    'DeviceId': '$DEVICE_ID',
    'Answer': '$ANSWER',
    'Timestamp': '$ts'
}))
")
  fi

  echo "Submitting response ${rid} ..."
  echo "  Device:   ${DEVICE_ID}"
  echo "  Question: ${QUESTION_ID}"
  echo "  Answer:   ${ANSWER}"
  echo "  Time:     ${ts}"

  local result
  result=$(curl -s -w "\n%{http_code}" \
    -X POST "${BASE_URL}/api/v1/responses" \
    -H "Content-Type: application/json" \
    -d "$json")

  local http_code
  http_code=$(echo "$result" | tail -1)
  local body
  body=$(echo "$result" | sed '$d')

  if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo "  -> Success (HTTP ${http_code})"
  else
    echo "  -> Error (HTTP ${http_code}): ${body}"
    return 1
  fi
}

echo "Submitting ${RESPONSE_COUNT} response(s) to ${BASE_URL} ..."
echo ""

failures=0
for i in $(seq 1 "$RESPONSE_COUNT"); do
  # Clear RESPONSE_ID for each iteration so a new UUID is generated,
  # unless the caller explicitly set it (in which case only 1 makes sense).
  if [ "$RESPONSE_COUNT" -gt 1 ]; then
    unset RESPONSE_ID 2>/dev/null || true
  fi

  if ! submit_one; then
    failures=$((failures + 1))
  fi
  echo ""
done

echo "Done. ${RESPONSE_COUNT} submitted, ${failures} failed."
[ "$failures" -eq 0 ] || exit 1
