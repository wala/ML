# [Ariadne](https://wala.github.io/ariadne/)

This is the top level repository for Ariadne code.  More information on using the Ariadne tools can be found [here](https://wala.github.io/ariadne/).  This repository is code to analyze machine learning code with WALA.  Currently, the code consists of the analysis of Python (com.ibm.wala.cast.python), analysis focused on machine learning in Python (com.ibm.wala.cast.python.ml), support for using the analysis via J2EE Websockets (com.ibm.wala.cast.python.ml.j2ee) and their associated test projects.

Since it is built using WALA, you need to have WALA on your system to use it:

* make sure Apache Maven and the Android SDK tools are installed, including build tools 26.0.2
* set ANDROID_HOME to the location of the Android SDK
* make sure JAVA_HOME points to a Java 8 JDK
* clone WALA with `git clone https://github.com/wala/WALA`
* in the cloned directory, `./gradlew clean build -x test`

Currently, the Python analysis code is being developed in Eclipse:

* import the WALA projects into Eclipse
* clone this repository, and import its projects into Eclipse
* run `TestCalls` in the `com.ibm.wala.cast.python.test` projects to test
