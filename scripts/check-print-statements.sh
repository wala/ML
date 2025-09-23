#!/bin/bash

# Script to check for inappropriate print statement usage
# Exits with code 1 if inappropriate print statements are found

set -e

# Define allowed directories/patterns for print statements
ALLOWED_PATTERNS=(
    "/driver/"            # CLI driver classes
    "/test/"              # Test classes
    "/test-source/"       # Test source directories
)

# Files with legitimate print usage for demonstration/main methods
LEGITIMATE_FILES=(
    "PythonFileParser.java"     # main method for demo
    "PythonModuleParser.java"   # main method for demo
)

# Create grep pattern to exclude allowed directories
EXCLUDE_PATTERN=""
for pattern in "${ALLOWED_PATTERNS[@]}"; do
    if [ -n "$EXCLUDE_PATTERN" ]; then
        EXCLUDE_PATTERN="$EXCLUDE_PATTERN|$pattern"
    else
        EXCLUDE_PATTERN="$pattern"
    fi
done

echo "Checking for inappropriate print statement usage..."
echo "Allowed patterns: ${ALLOWED_PATTERNS[*]}"
echo "Legitimate files: ${LEGITIMATE_FILES[*]}"

# Find Java files with System.out or System.err that are NOT in allowed directories
ALL_VIOLATIONS=$(find . -name "*.java" -type f | grep -vE "($EXCLUDE_PATTERN)" | xargs grep -l "System\.\(out\|err\)" 2>/dev/null || true)

# Filter out legitimate files
VIOLATIONS=""
for file in $ALL_VIOLATIONS; do
    is_legitimate=false
    for legit_file in "${LEGITIMATE_FILES[@]}"; do
        if [[ "$file" == *"$legit_file" ]]; then
            # Check if it's only in main method
            if grep -q "public static void main" "$file" && grep -A 20 "public static void main" "$file" | grep -q "System\.\(out\|err\)"; then
                is_legitimate=true
                break
            fi
        fi
    done
    
    if [ "$is_legitimate" = false ]; then
        if [ -n "$VIOLATIONS" ]; then
            VIOLATIONS="$VIOLATIONS\n$file"
        else
            VIOLATIONS="$file"
        fi
    fi
done

if [ -n "$VIOLATIONS" ]; then
    echo "ERROR: Found inappropriate print statement usage in the following files:"
    echo -e "$VIOLATIONS"
    echo ""
    echo "Print statements should only be used in CLI driver classes."
    echo "For non-CLI code, please use java.util.logging.Logger instead."
    echo ""
    echo "Files with violations:"
    for file in $(echo -e "$VIOLATIONS"); do
        echo "  $file:"
        grep -n "System\.\(out\|err\)" "$file" | head -3
        echo ""
    done
    exit 1
else
    echo "✓ No inappropriate print statement usage found"
    exit 0
fi