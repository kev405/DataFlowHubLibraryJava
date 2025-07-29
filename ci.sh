#!/usr/bin/env bash
set -euo pipefail
./mvnw -B clean verify        # incluye tests + cobertura + check