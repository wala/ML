# Contributing

## Building

### Installing Jython 3

You must install the `jython-dev.jar` to your local maven repository.

1. Clone the Jython 3 Git repo: `git clone https://github.com/juliandolby/jython3.git`.
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

1. Clone the [IDE repository][IDE].
1. Change directory to `com.ibm.wala.cast.lsp`: `cd com.ibm.wala.cast.lsp`
1. Build and install to your local Maven repo: `mvn install`

### Building WALA/ML

1. Clone the [WALA/ML repository][WALA/ML].
1. Build and install to your local Maven repo: `mvn install`

[SO post]: https://stackoverflow.com/questions/4955635/how-to-add-local-jar-files-to-a-maven-project#answer-4955695
[IDE]: https://github.com/wala/IDE
[WALA/ML]: https://github.com/wala/ML
