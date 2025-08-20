# [Ariadne](https://wala.github.io/ariadne/)

[![Continuous integration](https://github.com/wala/ML/actions/workflows/continuous-integration.yml/badge.svg)](https://github.com/wala/ML/actions/workflows/continuous-integration.yml) [![Dependabot Updates](https://github.com/wala/ML/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/wala/ML/actions/workflows/dependabot/dependabot-updates) [![codecov.io](https://codecov.io/gh/wala/ML/coverage.svg)](https://codecov.io/gh/wala/ML) 

This is the top level repository for Ariadne code. More information on using the Ariadne tools can be found [here](https://wala.github.io/ariadne/). This repository is code to analyze machine learning code with [WALA]. Currently, the code consists of the analysis of Python (`com.ibm.wala.cast.python`), analysis focused on machine learning in Python (`com.ibm.wala.cast.python.ml`), support for using the analysis via J2EE WebSockets (`com.ibm.wala.cast.python.ml.j2ee`) and their associated test projects.

Since it is built using [WALA], you need to have WALA on your system to use it. Instructions on building this project can be found in [CONTRIBUTING.md].

To test, for example, run `TestCalls` in the `com.ibm.wala.cast.python.test` project.

[WALA]: https://github.com/wala/WALA
[CONTRIBUTING.md]: CONTRIBUTING.md#building
