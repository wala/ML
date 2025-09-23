# Print Statement Usage Policy

This project enforces a policy that prevents inappropriate usage of `System.out.println()` and `System.err.println()` in non-CLI code. Print statements should only be used in CLI driver classes for user-facing output.

## Policy

- **Allowed**: Print statements in CLI driver classes (under `/driver/` packages)
- **Allowed**: Print statements in main methods of utility/demo classes (e.g., parser demos)
- **Not Allowed**: Print statements in core library code, analysis code, test classes, or other components

For all non-CLI code, including test classes, use `java.util.logging.Logger` instead of print statements.

## Build Integration

The build automatically checks for inappropriate print statement usage during the `validate` phase using Maven Checkstyle plugin:

```bash
# This will fail if inappropriate print statements are found
mvn validate

# This will succeed only if no violations are detected
mvn validate -Dskip.print.check=false
```

## Bypassing the Check

During development or when working on refactoring print statements (e.g., issue #331), you can skip the check:

```bash
# Skip the print statement check
mvn validate -Dskip.print.check=true

# Or set it permanently in your local settings
mvn clean install -Dskip.print.check=true
```

## Fixing Violations

If the build fails due to inappropriate print statements:

1. **For debug/info messages**: Replace with appropriate logging:
```java
// Bad
System.err.println("Debug info: " + value);

// Good
private static final Logger LOGGER = Logger.getLogger(MyClass.class.getName());
LOGGER.fine("Debug info: " + value);
```

2. **For error messages**: Use logging with appropriate levels:
```java
// Bad
System.err.println("Error occurred: " + exception.getMessage());

// Good
LOGGER.severe("Error occurred: " + exception.getMessage());
```

3. **For CLI output**: Move the code to a driver class or ensure it's in an appropriate location

## Script Details

The check is implemented using Maven Checkstyle plugin with a custom configuration that:

- Scans all Java files (including test files) for `System.out` and `System.err` usage
- Excludes files in `/driver/` directories
- Allows print statements in main methods of specific utility classes
- Fails the build if violations are found

## Examples

### ✅ Allowed Usage

```java
// CLI driver class
public class Ariadne {
	public static void main(String[] args) {
		System.out.println("Analysis complete."); // OK - CLI output
	}
}

// Test class
public class TestParser {
	private static final Logger LOGGER = Logger.getLogger(TestParser.class.getName());
	
	public void testMethod() {
		LOGGER.info("Debug output"); // OK - using logging
	}
}

// Demo main method
public class PythonFileParser {
	public static void main(String[] args) {
		System.err.println(script); // OK - demo/utility main method
	}
}
```

### ❌ Prohibited Usage

```java
// Core library code
public class PythonParser {
	public void parseCode() {
		System.err.println("Parsing..."); // NOT OK - use LOGGER.fine() instead
	}
}

// Analysis engine
public class AnalysisEngine {
	public void analyze() {
		System.out.println("Found result"); // NOT OK - use LOGGER.info() instead
	}
}

// Test class
public class TestAnalysis {
	public void testMethod() {
		System.err.println("Debug info"); // NOT OK - use LOGGER.info() instead
	}
}
```
