#!/bin/bash

# Test script to validate that the print statement checker works correctly

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TEMP_DIR="/tmp/print-check-test"

echo "Testing print statement checker..."

# Clean up any previous test
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Create a test Java file with violations
cat > "$TEMP_DIR/TestViolation.java" << 'EOF'
package test;

public class TestViolation {
    public void method() {
        System.out.println("This should be flagged");
    }
}
EOF

# Create a test Java file without violations (in driver package)
mkdir -p "$TEMP_DIR/driver"
cat > "$TEMP_DIR/driver/TestDriver.java" << 'EOF'
package test.driver;

public class TestDriver {
    public static void main(String[] args) {
        System.out.println("This is OK - CLI output");
    }
}
EOF

# Copy the checker script
cp "$PROJECT_ROOT/scripts/check-print-statements.sh" "$TEMP_DIR/"

# Test 1: Should find violations
echo "Test 1: Testing with violations..."
cd "$TEMP_DIR"
if ./check-print-statements.sh; then
    echo "ERROR: Expected script to fail but it passed"
    exit 1
else
    echo "✓ Script correctly detected violations"
fi

# Test 2: Remove violations and test again
rm TestViolation.java
echo "Test 2: Testing without violations..."
if ./check-print-statements.sh; then
    echo "✓ Script correctly passed when no violations found"
else
    echo "ERROR: Expected script to pass but it failed"
    exit 1
fi

echo "✓ All tests passed!"

# Clean up
rm -rf "$TEMP_DIR"