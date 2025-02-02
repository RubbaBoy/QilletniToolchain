#!/bin/bash
# /opt/qilletni/run_docs_wrapper.sh
#
# This wrapper is run as a forced command from the deploy key.
# It retrieves the originally requested command from SSH_ORIGINAL_COMMAND,
# validates that it is a simple library name (only letters, numbers, underscores, or hyphens),
# and then calls run_docs.sh with that argument.
#
# SECURITY: This script enforces that SSH_ORIGINAL_COMMAND contains no spaces or unsafe characters.
# You may add further validation (for example, checking against allowed_releases.json) if desired.

set -euo pipefail

if [ -z "${SSH_ORIGINAL_COMMAND:-}" ]; then
  echo "Error: No command provided." >&2
  exit 1
fi

# Validate that the command contains only allowed characters (alphanumerics, underscore, hyphen)
if [[ "${SSH_ORIGINAL_COMMAND}" =~ ^[a-zA-Z0-9_-]+$ ]]; then
  LIBRARY_NAME="${SSH_ORIGINAL_COMMAND}"
else
  echo "Error: Invalid library name '${SSH_ORIGINAL_COMMAND}'." >&2
  exit 1
fi

# Optionally, you can cross-check the LIBRARY_NAME against your allowed_releases.json here.
# For example:
if ! grep -q "\"name\": \"$LIBRARY_NAME\"" /opt/qilletni/allowed_releases.json; then
  echo "Error: Library '$LIBRARY_NAME' is not allowed." >&2
  exit 1
fi

exec sudo /opt/qilletni/run_docs.sh "$LIBRARY_NAME"
