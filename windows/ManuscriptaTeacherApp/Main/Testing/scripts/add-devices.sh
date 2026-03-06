#!/usr/bin/env bash
# add-devices.sh — Generate dummy Android devices via the simulation API.
#
# Usage:
#   ./add-devices.sh [COUNT] [BASE_URL]
#
# Parameters:
#   COUNT     Number of devices to create (default: 5)
#   BASE_URL  Server base URL (default: http://localhost:5911)
#
# Examples:
#   ./add-devices.sh              # Create 5 devices
#   ./add-devices.sh 10           # Create 10 devices
#   ./add-devices.sh 3 http://192.168.1.50:5911
#
# The server must be running with the SimulationController available.
# Each device is registered, set to ON_TASK status, and the frontend is
# notified via SignalR (DevicePaired broadcast).
#
# Output: JSON containing the created device IDs and names.

set -euo pipefail

COUNT="${1:-5}"
BASE_URL="${2:-http://localhost:5911}"

echo "Creating ${COUNT} dummy device(s) on ${BASE_URL} ..."

response=$(curl -s -w "\n%{http_code}" \
  -X POST "${BASE_URL}/api/simulation/add-device?count=${COUNT}")

http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
  echo "Success (HTTP ${http_code}):"
  echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
else
  echo "Error (HTTP ${http_code}):"
  echo "$body"
  exit 1
fi
