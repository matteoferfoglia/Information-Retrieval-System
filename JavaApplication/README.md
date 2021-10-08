Information Retrieval System - Java Application
===============================================

In this repository you can find a Java application that implements an information retrieval system.

## Execution

After having created the application with the command

    mvn package

which must be run from the terminal placed at the root folder of the project (where the file `pom.xml` is placed), it is
possible to execute the application with the following command:

    mvn exec:java

Alternatively, if you want to execute the application as part of the Maven lifecycle, you can use the command

    mvn install

which will install the project into your local Maven repository and run the application.

**Notice**: when using the Maven phase `mvn install` instead of
`mvn exec:java`, all user-specific eventually saved properties will be lost. At the same way, when using `mvn package`,
all user-specific eventually saved properties will be lost. If you have any user-specific property, simply
use `mvn exec:java`.

## Distribution

This project is configured to create, in addition to the JAR file obtained from the package phase, a further larger
file, inside which all the dependencies required by the project have been assembled. This JAR file can be directly
distributed to the users, who can execute it from the terminal (assuming they have installed the correct version of Java
8) with the command

    java -jar *jarFileName.jar* *inputParameters*