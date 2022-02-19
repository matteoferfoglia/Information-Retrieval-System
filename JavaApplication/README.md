Information Retrieval System - Java Application
===============================================

In this repository you can find a Java application that implements an information retrieval system. The application is
designed for textual documents and implements the Boolean Model and is able to answer:

- Boolean queries with AND, OR, and NOT.
- Wildcard and phrase queries.

The application implements mechanism for spelling and phonetic corrections. The application can normalize text and can
use stemming (see the *stemmers* package).

The application is extensible (further features can be added). For example, the user can use this system with any
textual collection of documents:
the user must simply define (at least) two classes:

1) one extending the class Document (the extended class will be a concrete description of each document belonging to the
   collection tha the user wants to use)
2) one extending the class CorpusFactory (that describes how to formally represent the corpus).

## Environment properties

The application support the configuration of some environment properties
(see file *properties.default.env* in the main resource folder).

## Hardware / Software Requirements

The Java Application does not impose hard requirements to be executed (assuming that a JAR file is already provided),
but require Java 16 or higher.

Building the Java Application requires Maven 3 (and an Internet connection to eventually download missing dependencies).
Test phase (and benchmark tests in particular) requires a lot of hardware resource (as pointed to in the following
paragraph about benchmarking).

## Execution

After having created the application with the command

    mvn package

which must be run from the terminal placed at the root folder of the project (where the file `pom.xml` is placed), it is
possible to execute the application with the following command:

    mvn exec:java@main

Alternatively, if you want to execute the application as part of the Maven lifecycle, you can use the command

    mvn install

which will install the project into your local Maven repository and run the application.

## Distribution

This project is configured to create, in addition to the JAR file obtained from the package phase, a further larger
file, inside which all the dependencies required by the project have been assembled. This JAR file can be directly
distributed to the users, who can execute it from the terminal (assuming they have installed the correct version of
Java, as required by this project) with the command

    java -jar *jarFileName.jar* *inputParameters*

The JAR files is produced together with the *package* phase (i.e., with the command `mvn package`).

## Test

This project is provided with a number (~hundreds) of unit tests which improve the reliability about the correctness of
the source code. The used test framework is *JUnit 5*. Unit tests can be run with the command

    mvn test

### Evaluation

The Information Retrieval System realized in this project has been evaluated according to different evaluation metrics,
in particular:

- Precision
- Recall
- 11-point interpolated average precision
- MAP
- Precision@K
- R-precision Some metrics propose a plot to summarize results, too.

### Benchmark

This project use the Java-Benchmark framework (see dependencies in *pom.xml*)
for benchmarking methods. The Benchmark tests can be run with the command

    mvn exec:java@benchmark

**Notice**: benchmark tests can be run only after having compiled test classes
(Maven phase "test-compile", at least); the entire command to be sure that benchmark tests succeed
is `mvn clean test exec:java@benchmark` (note that this command will also clean the entire target directory and
recompile the entire application).

Executing benchmark tests may require a lot of hardware resources (RAM, CPU, ...) and it might eventually fail (e.g.,
insufficient RAM). I recommend using a computer with >= 16 GB RAM and high performance CPU.

### test.py

This project provides a Python (version 3) script (named *test.py*) which can be used to test the entire project (with
all possible values for the environment variables)
in order to test the system in every operating condition. The Python script can eventually be updated abd allow to
specify (thanks to a configurable flag) if benchmark tests have to be run. Executing the Python script may require a
number of *hours* and a lot of hardware resources (RAM, CPU, ...) and it might eventually fail (e.g., insufficient RAM).