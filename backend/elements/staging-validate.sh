#!/bin/bash

set -e

xmllint --noout --nonet \
  --schema XwalkOut.xsd \
  staging-deposit-xwalkout.xml

xmllint --noout --nonet \
  --schema XwalkIn.xsd \
  staging-harvest-xwalkin.xml
