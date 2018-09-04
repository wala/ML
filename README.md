# [Ariadne](https://wala.github.io/ariadne/)

This is the top level repository for creating the Ariadne framework.  More information on Ariadne can be found [here](https://wala.github.io/ariadne/)

# ML

This repository is for code to analyze machine learning code using WALA.  Currently, the code consists of the 
beginnings of analysis of Python.  

Since it is built using WALA, you need to have WALA on your system to use it:

* make sure Apache Maven and the Android SDK tools are installed, including build tools 26.0.2
* set ANDROID_HOME to the location of the Android SDK
* make sure JAVA_HOME points to a Java 8 JDK
* clone WALA with `git clone https://github.com/wala/WALA`
* in the cloned directory, `mvn clean install -DskipTests`

Currently, the Python analysis code is being developed in Eclipse:

* import the WALA projects into Eclipse
* clone this repository, and import its projects into Eclipse
* run `TestCalls` in the `com.ibm.wala.cast.python.test` projects to test
