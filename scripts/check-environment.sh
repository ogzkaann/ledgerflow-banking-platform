#!/usr/bin/env bash
set -euo pipefail

for command_name in java git; do
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Missing required command: ${command_name}" >&2
    exit 1
  fi
done

java_major=$(java -XshowSettings:properties -version 2>&1 \
  | awk -F'= ' '/java.specification.version/ { print $2; exit }')

if [[ "${java_major}" != "25" ]]; then
  echo "Java 25 is required; found Java ${java_major}." >&2
  exit 1
fi

if [[ ! -x ./mvnw ]]; then
  echo "Run this script from the repository root." >&2
  exit 1
fi

echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Git: $(git --version)"
./mvnw --version
echo "Environment check completed."
