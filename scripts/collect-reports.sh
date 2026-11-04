#!/usr/bin/env bash
set -euo pipefail
mkdir -p /workspace/reports
for module in tracker-*; do
  if [ -d "$module/target" ]; then
    mkdir -p "/workspace/reports/$module"
    for report in surefire-reports failsafe-reports site screenshots checkstyle-result.xml pmd.xml cpd.xml; do
      if [ -e "$module/target/$report" ]; then cp -R "$module/target/$report" "/workspace/reports/$module/"; fi
    done
  fi
done

if [ -f /workspace/target/cpd.xml ]; then
  cp /workspace/target/cpd.xml /workspace/reports/aggregate-cpd.xml
fi

# Maven runs as root in the test image; Linux callers must own exported reports.
if [[ -n "${REPORT_UID:-}${REPORT_GID:-}" ]]; then
  if ! [[ "${REPORT_UID:-}" =~ ^[0-9]+$ && "${REPORT_GID:-}" =~ ^[0-9]+$ ]]; then
    printf '%s\n' 'REPORT_UID and REPORT_GID must both be numeric.' >&2
    exit 2
  fi
  chown -R -- "$REPORT_UID:$REPORT_GID" /workspace/reports
fi
