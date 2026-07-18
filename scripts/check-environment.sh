#!/usr/bin/env bash
set -euo pipefail

required_commands=(java docker git)

for command_name in "${required_commands[@]}"; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Missing required command: ${command_name}" >&2
    exit 1
  fi
done

java_version=$(java -version 2>&1 | head -n 1)
echo "Java: ${java_version}"
echo "Docker: $(docker --version)"
echo "Git: $(git --version)"

echo "Environment check completed."
