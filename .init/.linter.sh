#!/bin/bash
cd /home/kavia/workspace/code-generation/smart-to-do-list-303826-303836/todo_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

