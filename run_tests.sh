#!/bin/bash

# Define API Gateway host and port (modify if needed)
HOST="localhost"
PORT=8081

# Check if macOS or Linux
OS="$(uname)"
if [[ "$OS" == "Darwin" ]]; then
    echo "Running on macOS"
elif [[ "$OS" == "Linux" ]]; then
    echo "Running on Linux"
else
    echo "Unsupported OS: $OS"
    exit 1
fi

# Define an array of endpoints
endpoints=(
    "api-gateway/overload?input=test"
    "chatty/service-a?input=test"
    "cyclic/cyclic-a?input=test"
    "eventual-consistency/write?input=test"
    "eventual-consistency/read"
    "knot-a?input=test"
    "long-chain/start?input=test"
    "nano-service?input=test"
    "fan-in/service-a?input=test"
    "fan-in/service-b?input=test"
    "fan-in/service-c?input=test"
    "fan-out/service-main?input=test"
    "sync-overuse/service?input=test"
)

# Output file for logs
LOG_FILE="curl_responses.log"
echo "Running API tests..." > "$LOG_FILE"

# Loop through endpoints and execute curl
for endpoint in "${endpoints[@]}"; do
    echo "Testing: http://$HOST:$PORT/$endpoint"
    response=$(curl -s -w "\nHTTP_CODE: %{http_code}" -X GET "http://$HOST:$PORT/$endpoint")
    echo -e "\nEndpoint: $endpoint\n$response" | tee -a "$LOG_FILE"
    echo "------------------------------------------------------" | tee -a "$LOG_FILE"
done

echo "Testing complete. Responses saved in $LOG_FILE"
