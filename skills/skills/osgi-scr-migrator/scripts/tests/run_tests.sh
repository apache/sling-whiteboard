#!/bin/bash
#
# Run all migration tests
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "======================================================================"
echo "Running OSGi SCR Migration Tests"
echo "======================================================================"
echo

# Run annotation migration tests
echo "Running annotation migration tests..."
python3 test_migrate_annotations.py
RESULT1=$?

echo
echo "----------------------------------------------------------------------"
echo

# Run POM migration tests
echo "Running POM migration tests..."
python3 test_migrate_pom.py
RESULT2=$?

echo
echo "----------------------------------------------------------------------"
echo

# Run constructor injection migration tests
echo "Running constructor injection migration tests..."
python3 test_migrate_constructor_injection.py
RESULT3=$?

echo
echo "======================================================================"
if [ $RESULT1 -eq 0 ] && [ $RESULT2 -eq 0 ] && [ $RESULT3 -eq 0 ]; then
    echo "✓ All tests passed!"
    echo "======================================================================"
    exit 0
else
    echo "✗ Some tests failed"
    echo "======================================================================"
    exit 1
fi
