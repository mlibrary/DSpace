#!/bin/bash

set -eu

if ! command -v xmllint >/dev/null 2>&1; then
  echo "Error: xmllint is required and must be available on PATH." >&2
  exit 1
fi

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

xmllint --noout --nonet \
  --schema "$script_dir/schemas/XwalkOut.xsd" \
  "$script_dir/dspace-XwalkOut-map.xml"

xmllint --noout --nonet \
  --schema "$script_dir/schemas/XwalkIn.xsd" \
  "$script_dir/dspace-XwalkIn-map.xml"
