# Ariadne - WALA Python/ML Analysis Framework

Ariadne is a static analysis framework for Python and Machine Learning code built on IBM WALA. It provides Python call graph analysis, ML-specific analysis, and Language Server Protocol (LSP) support for editor integration.

**ALWAYS reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Working Effectively

### Bootstrap and Build
Execute these commands in order for a complete setup:

```bash
# 1. Initialize git submodules (3 minutes - NEVER CANCEL)
git submodule update --init --recursive

# 2. Install Python dependencies (30 seconds)
pip install -r requirements.txt

# 3. Set Java environment (required: Java 21 for compilation)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 4. Build Jython3 (26 seconds - NEVER CANCEL)
cd jython3
ant
cd dist
mvn install:install-file \
-Dfile=./jython-dev.jar \
-DgroupId="org.python" \
-DartifactId="jython3" \
-Dversion="0.0.1-SNAPSHOT" \
-Dpackaging="jar" \
-DgeneratePom=true -B
cd ../..

# 5. Build IDE/LSP component (17 seconds - NEVER CANCEL)
cd IDE/com.ibm.wala.cast.lsp
mvn install -B -q -DskipTests
cd ../..

# 6. Build main project (40 seconds - NEVER CANCEL, set timeout to 120+ seconds)
mvn install -B -DskipTests
```

### CRITICAL Build Notes
- **NEVER CANCEL** any build command. Set timeouts to 120+ seconds minimum for Maven builds.
- **WALA Dependency**: The project requires WALA but building WALA from source needs Java 24 (not available in standard environments). The build works fine using pre-built WALA 1.6.12 from Maven Central.
- **Java Version**: Use Java 21 for the ML project. Java 24 is only needed if building WALA from source.
- **Test Failures**: Tests may fail due to missing full WALA build dependencies. Always use `-DskipTests` for reliable builds.

## Validation and Testing

### Format and Lint Checks
```bash
# Java code formatting check (19 seconds - NEVER CANCEL, set timeout to 60+ seconds)
mvn spotless:check -B

# Python code formatting check (3 seconds)
black --fast --check .

# Apply formatting fixes
mvn spotless:apply -B
black .
```

### Running Tests
```bash
# Compile only (42 seconds - NEVER CANCEL, set timeout to 120+ seconds)
mvn compile -B

# Limited tests (some will fail due to WALA dependencies)
mvn test -B -Dtest="TestCalls"

# Skip all tests for reliable CI build
mvn install -B -DskipTests
```

### Ariadne CLI Tool
```bash
# Build creates shaded JAR for CLI usage
java -cp ml/com.ibm.wala.cast.python.ml/target/com.ibm.wala.cast.python.ml-0.0.1-SNAPSHOT-shaded.jar \
com.ibm.wala.cast.python.ml.driver.Ariadne --help

# Note: Full functionality requires complete WALA build dependencies
```

## Manual Validation Requirements
After making changes, always run through this validation sequence:

1. **Build Validation**: Run full build sequence above and verify no compilation errors
2. **Format Validation**: Run both spotless and black checks to ensure code style compliance
3. **CLI Testing**: Verify Ariadne CLI shows help output correctly
4. **Import Testing**: Test basic Java compilation to verify dependencies resolve

## Timing Expectations

| Step | Time | Timeout | Critical Notes |
|------|------|---------|----------------|
| Git submodules | 3 minutes | 300+ seconds | One-time setup, NEVER CANCEL |
| Jython3 build | 26 seconds | 120+ seconds | NEVER CANCEL |
| Jython3 install | 6 seconds | 60+ seconds | Maven install step |
| IDE build | 17 seconds | 60+ seconds | NEVER CANCEL |
| Main build | 40 seconds | 120+ seconds | NEVER CANCEL |
| Spotless check | 19 seconds | 60+ seconds | NEVER CANCEL |
| Black check | 3 seconds | 30+ seconds | Fast Python format check |

**Total clean build time: ~2 hours (including submodules) or ~90 seconds (if submodules exist)**

## Repository Structure

### Key Components
- **core/**: Python analysis core (`com.ibm.wala.cast.python`)
- **ml/**: ML-specific analysis (`com.ibm.wala.cast.python.ml`)
- **jython/**: Jython-based Python parsing
- **jep/**: JEP (Java Embedded Python) integration
- **IDE/**: Language Server Protocol implementation
- **WALA/**: WALA static analysis framework (git submodule)
- **jython3/**: Jython 3 implementation (git submodule)

### Important Files
- `pom.xml`: Root Maven project configuration
- `requirements.txt`: Python dependencies (ast2json, jep, black)
- `CONTRIBUTING.md`: Detailed build instructions
- `.github/workflows/continuous-integration.yml`: CI pipeline configuration

## Common Tasks

### Building for Development
```bash
# Quick compile check
mvn compile -B

# Full build with format checks
mvn spotless:apply -B && black . && mvn install -B -DskipTests
```

### CI Pipeline Preparation
The project's CI pipeline expects this sequence:
1. Git submodules initialized
2. Python dependencies installed
3. Jython3 built and installed
4. WALA built (or using Maven Central version)
5. IDE component built
6. Main project built with tests

Always run `mvn spotless:apply -B` and `black .` before committing to avoid CI failures.

## Troubleshooting

### Common Issues
- **Java version errors**: Ensure Java 21 is set in JAVA_HOME and PATH
- **WALA build failures**: Use pre-built WALA from Maven Central (default behavior)
- **Test failures**: Expected due to incomplete dependencies, use `-DskipTests`
- **Formatting failures**: Run `mvn spotless:apply -B` and `black .` to auto-fix
- **Submodule issues**: Re-run `git submodule update --init --recursive`

### Network Dependencies
- Maven Central for Java dependencies
- Python Package Index for Python dependencies
- GitHub for git submodules (WALA, IDE, jython3)

## Project Context
This is a research framework for static analysis of Python code with focus on Machine Learning applications. The main deliverable is the Ariadne tool which can be used as:
- Command-line linter for Python/ML code
- Language Server Protocol server for editor integration
- Analysis framework for research applications

The codebase integrates multiple technologies (Java, Python, Jython) and requires careful dependency management between WALA, Jython, and the ML-specific components.
