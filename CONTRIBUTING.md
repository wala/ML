# Contributing

## Building

### Obtain Git Submodules

The following dependencies require Git submodules to be initialized and updated. If you cloned the repository without the `--recurse-submodules` option, you need to initialize and update the submodules:

```bash
git submodule update --init --recursive
```

### Installing Jython 3

You must install the `jython-dev.jar` to your local maven repository.

1. Change directory to the cloned local Git repo: `cd jython3`.
1. Build Jython 3: `ant`. That will produce the file `jython3/dist/jython-dev.jar`.
1. Install the `jython-dev.jar` into your local maven repo (see [this post][SO post]):

	```bash
	mvn install:install-file \
	-Dfile=./jython-dev.jar \
	-DgroupId="org.python" \
	-DartifactId="jython3" \
	-Dversion="0.0.1-SNAPSHOT" \
	-Dpackaging="jar" \
	-DgeneratePom=true
	```
### Installing IDE

1. Change directory to `com.ibm.wala.cast.lsp`: `cd com.ibm.wala.cast.lsp`
1. Build and install to your local Maven repo: `mvn install`

### Building WALA/ML

Build and install to your local Maven repo: `mvn install`

## Code Quality Standards

This project enforces code quality standards during the build process:

### Print Statement Policy

Print statements (`System.out.println`, `System.err.println`) are only allowed in CLI driver classes. The build will fail if inappropriate print statements are detected in core library code.

- **To fix violations**: Replace print statements with appropriate logging using `java.util.logging.Logger`
- **To skip check during development**: Use `mvn install -Dskip.print.check=true`
- **For detailed policy**: See [Print Statement Policy](docs/PRINT_STATEMENT_POLICY.md)

### Code Formatting

- **Java**: Uses Spotless with Google Java Format - run `mvn spotless:apply` to auto-fix
- **Python**: Uses Black - run `black .` to auto-fix

[SO post]: https://stackoverflow.com/questions/4955635/how-to-add-local-jar-files-to-a-maven-project#answer-4955695
