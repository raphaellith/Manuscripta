#!/bin/bash

# Start both frontend prototypes concurrently

echo "Starting Manuscripta frontends..."

# Start tablet UI prototype on port 3000
cd manuscripta-tablet-ui-prototype && npm run dev -- --port 3001 &
TABLET_PID=$!

# Start teacher portal prototype on port 3001
cd manuscripta-teacher-portal-prototype && npm run dev -- --port 3000 &
TEACHER_PID=$!

echo ""
echo "📱 Tablet UI:       http://localhost:3001"
echo "🖥️  Teacher Portal: http://localhost:3000"
echo ""
echo "Press Ctrl+C to stop both servers"

# Wait for both processes and handle Ctrl+C
trap "kill $TABLET_PID $TEACHER_PID 2>/dev/null; exit" SIGINT SIGTERM
wait
