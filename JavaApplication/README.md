Information Retrieval System - Java Application
===============================================

In this repository you can find a Java application that implements an Information Retrieval System. The application is
designed for textual documents. It implements the Boolean Model and it is able to answer:

- Boolean queries with AND, OR, and NOT operators;
- Wildcard and phrase queries.

The application implements mechanisms for spelling and phonetic corrections. The application can normalize the text and
can use stemming (see the inner *stemmers* package). The application tries to rank results obtained from the evaluation
of a query (wf-idf metric is used, together with some heuristics that may assign higher scores to some resulting
documents).

The application is extensible (further features can be added). For example, the user can use this system with any
textual collection of documents, without modifying the system structure:
the user must simply define (at least) two classes:

1) one extending the class Document (the extended class will be a concrete description of each document belonging to the
   collection tha the user wants to use)
2) one extending the class CorpusFactory (that describes how to formally represent the corpus).

## Environment properties

The application support the configuration of some environment properties
(see file *properties.default.env* in the main resource folder).

## Hardware / Software Requirements

The Java Application itself does not impose hard requirements to be executed (assuming that a JAR file is already
provided), but requires Java 16 or higher.

Building the Java Application requires Maven 3.5 (and an Internet connection to eventually download missing
dependencies). The test-phase (benchmark tests in particular) requires a lot of hardware resources (as pointed to in the
following paragraph about benchmarking, see below).

## Maven

This project makes use of Maven 3.5 as build automation tool. All Maven commands (i.e., `mvn ...`) must be executed in a
terminal placed in the root directory of the project
(i.e., in the directory where the file *pom.xml* is placed).

## Execution

After having created the application with the command

    mvn package

which must be run from the terminal placed at the root folder of the project (where the file `pom.xml` is placed), it is
possible to execute the application with the following command:

    mvn exec:java@main

See also the paragraph about the distribution of the project, which explains how to create and execute the distributable
software.

## Distribution

This project is configured to create, in addition to the JAR file obtained from the package phase, a further larger JAR
file, inside which all the dependencies required by the project have been assembled. The latter JAR file can be directly
distributed to the users, who can run it from the terminal (assuming they have installed the correct version of Java, as
required by this project) with the command

    java -jar *jarFileName.jar* *inputParameters*

JAR files are produced together with the *package* phase (i.e., with the command `mvn package`).

## Test and Evaluation

This project is provided with a number (~hundreds) of unit tests which improve the reliability about the correctness of
the source code. The used test framework is *JUnit 5*. Unit tests can be run with the command

    mvn test

### Evaluation

The Information Retrieval System realized in this project has been evaluated according to the following evaluation
metrics:

- Precision
- Recall
- 11-point interpolated average precision
- MAP (Mean Average Precision)
- Precision@K
- R-precision

Some metrics propose a plot to summarize results.

### Benchmark

This project uses the Java-Benchmark framework (see dependencies in *pom.xml*)
for benchmarking of methods. The benchmark tests can be run with the command

    mvn exec:java@benchmark

**Notice**: benchmark tests can be run only after having compiled test classes
(at least the Maven "test-compile" phase must be done); the entire command to use to be sure that benchmark tests
provide meaningful results is `mvn clean test exec:java@benchmark` (note that this command will also clean the entire
target directory and recompile the entire project).

Executing benchmark tests may require a lot of hardware resources (RAM, CPU, ...) and it might eventually fail (e.g.,
insufficient RAM). I recommend using a computer with >= 16 GB of RAM and a high performance CPU.

### test.py

This project provides a Python (version 3) script (named *test.py*) which can be used to test the entire project (with
all possible values for the environment variables) in order to test the system in all operating conditions. The Python
script can eventually be updated to allow specifying (thanks to a variable of the Python script) if benchmark tests have
to be run. Executing the Python script may require a number of *hours* and a lot of hardware resources (RAM, CPU, ...)
and it might eventually fail (e.g., insufficient RAM), as described in the paragraph about benchmarking.

## Saving and loading an IR system

This project allows saving (serializing) an Information Retrieval system (after having been created) to the hard-disk
memory, in order to load (deserialize) it at a future time, without re-creating it. This is done because, in order to
create an Information Retrieval system, a corpus must be indexed, and this can be quite expensive.

## Example

An example in which an Information Retrieval system is created from a corpus of movies and then used is showed below.
Note that in this example the user tries to load an already existing system, but the user interface replies that no IR
systems are available and suggests creating one according to the available collections of documents (specified in the
resource folder of the project). After the creation, the user interface propose to save the system in order to be loaded
at a future time. Note also that if too many results are returned from a query, the user interface shows only a subset
of them and asks if the user wants to see more. Lines starting with '>' show the command inserted into the terminal to
start the application. In this example, lines have been cut to 105 characters (this example has only illustrative
purposes).

```
>java -jar ir_boolean_model-1.0.0-jar-with-dependencies.jar

=========================================================================================
=====                  Information Retrieval System - Boolean Model                  ====
=========================================================================================


Insert 'C' to crate a new IR system or 'L' to load an already existing one: l

No IR Systems are available
Available collections to index:
1)     Cranfield collections
2)     Movie corpus
Insert the number of the collection that you want to index: 2
Creating IR system, please wait...
Indexing started
4.15 %   11.33 %         18.02 %         24.25 %         29.4 %          35.45 %         41.35 %        
Indexing ended
Do you want to save the IR system to file? [y/n]: y
File "workingDirectory\irs\Movie corpus" created.
Serializing and saving the system on file, please wait...
IR System saved to file workingDirectory\irs\Movie corpus

System ready

INSTRUCTIONS
Insert:
'-p' before the query string to enable the phonetic correction,
'-s' before the query string to enable the spelling correction,
'-a' before the query string to enable the automatic "word-wise"
spelling correction, performed directly by the system,
without specifying any further parameters (automatic
correction will work only if no results are found by the
system for the given query),
the query string without any flags to retrieve exact matches only.
A combination of the previous flags can be used together. For phonetic
and spelling corrections, the number of corrections to attempt can be
specified by adding the number (the edit distance) after the flag (e.g.,
-p2 foo&bar
means to try to correct (phonetic correction) two times; this does
not mean that the final query string (after the correction) will have
edit distance of 2 from the initial query string, but it means that
it will have an edit distance of at least 2, in fact, suppose that
no corrections are available at distance 1, so the system automatically
increases the edit-distance to 2 and re-try: after one single attempt
of correction, in this example, the overall edit-distance will be 2,
but in the input string we specified to try to correct 2 times, hence
another correction attempt will be made and the resulting edit distance
will be >=2).
The query string can have AND, OR or NOT operations (use the symbols '&',
'|', '!' respectively, spaces are interpreted as AND queries).
The system recognizes parenthesis, which can be used in any query string
to specify the precedences.

Insert a query or '-q' to exit:
space jam
14 results found in 32.6172 ms
First 10 results:
- {"Space Jam": {"Title":"Space Jam","Release date":"1996-11-10T00:00:00Z","Box office revenue":
- {"Dr. No": {"Title":"Dr. No","Release date":"1962-10-05T00:00:00Z","Box office revenue":"59600
- {"The Sandlot 2": {"Title":"The Sandlot 2","Release date":"2005-05-03T00:00:00Z","Box office r
- {"Box Office Bunny": {"Title":"Box Office Bunny","Release date":"1990-01-01T00:00:00Z","Box of
- {"Stony Island": {"Title":"Stony Island","Release date":"1978-01-01T00:00:00Z","Box office rev
- {"Rock School": {"Title":"Rock School","Release date":"2005-06-03T00:00:00Z","Box office reven
- {"Robotech: The Shadow Chronicles": {"Title":"Robotech: The Shadow Chronicles","Release date":
- {"The Day After Tomorrow": {"Title":"The Day After Tomorrow","Release date":"2004-05-17T00:00:
- {"Quantum Apocalypse": {"Title":"Quantum Apocalypse","Release date":"2010-02-24T00:00:00Z","Bo
- {"The Time Machine": {"Title":"The Time Machine","Release date":"2002-03-04T00:00:00Z","Box of
4 results not showed yet. Do you want to see more results? [y/n]: n

Insert a query or '-q' to exit:
-a yioda lukke darhth
6 results found in 86.771301 ms
"darhth" not found, corrected with "darth"
"lukk" not found, corrected with one of the following: "luck", "luk", "luka", "luke", "luki", "lukka", "
"yioda" not found, corrected with "yoda"
- {"Something, Something, Something Dark Side": {"Title":"Something, Something, Something Dark S
- {"Star Wars Episode V: The Empire Strikes Back": {"Title":"Star Wars Episode V: The Empire Str
- {"Star Wars Episode III: Revenge of the Sith": {"Title":"Star Wars Episode III: Revenge of the
- {"It\'s a Trap!": {"Title":"It\\'s a Trap!","Release date":null,"Box office revenue":null,"Run
- {"Star Wars Episode VI: Return of the Jedi": {"Title":"Star Wars Episode VI: Return of the Jed
- {"Return of the Ewok": {"Title":"Return of the Ewok","Release date":"1982-01-01T00:00:00Z","Bo

Insert a query or '-q' to exit:
-p2 -s3 spes jem
4223 results found in 2541.417 ms for ( ( ( ( ( ( jem OR ( ( ( ( ( ( ( ( ( ( ( ( ( ( ( ( ( janewai OR ja
First 10 results:
- {"The Water Margin": {"Title":"The Water Margin","Release date":"1972-01-01T00:00:00Z","Box of
- {"The Goddess of 1967": {"Title":"The Goddess of 1967","Release date":"2000-01-01T00:00:00Z","
- {"Come Back to the Five and Dime, Jimmy Dean, Jimmy Dean": {"Title":"Come Back to the Five and
- {"Conan the Destroyer": {"Title":"Conan the Destroyer","Release date":"1984-06-29T00:00:00Z","
- {"The Whole Nine Yards": {"Title":"The Whole Nine Yards","Release date":"2000-02-18T00:00:00Z"
- {"Jonny Quest vs. The Cyber Insects": {"Title":"Jonny Quest vs. The Cyber Insects","Release da
- {"To Save a Life": {"Title":"To Save a Life","Release date":"2010-01-22T00:00:00Z","Box office
- {"Jhoom Barabar Jhoom": {"Title":"Jhoom Barabar Jhoom","Release date":"2007-06-15T00:00:00Z","
- {"Juno": {"Title":"Juno","Release date":"2007-09-01T00:00:00Z","Box office revenue":"231411584
- {"Marathon Man": {"Title":"Marathon Man","Release date":"1976-10-06T00:00:00Z","Box office rev
4213 results not showed yet. Do you want to see more results? [y/n]: n

Insert a query or '-q' to exit:
"Space jam"
2 results found in 2.312001 ms
- {"Space Jam": {"Title":"Space Jam","Release date":"1996-11-10T00:00:00Z","Box office revenue":
- {"Box Office Bunny": {"Title":"Box Office Bunny","Release date":"1990-01-01T00:00:00Z","Box of

Insert a query or '-q' to exit:
-q

>java -jar ir_boolean_model-1.0.0-jar-with-dependencies.jar

=========================================================================================
=====                  Information Retrieval System - Boolean Model                  ====
=========================================================================================


Insert 'C' to crate a new IR system or 'L' to load an already existing one: l

Available IR Systems:
1)     Movie corpus
Insert the number of the IRS that you want to load: 1
Loading the IRSystem from the file system, please wait...
0.1%    15.61%  24.56%  32.54%  40.87%  47.92%  56.34%  63.27%  71.01%  78.04%  86.96%  92.27%  
IRSystem loaded from the file system.

System ready

INSTRUCTIONS
Insert:
'-p' before the query string to enable the phonetic correction,
'-s' before the query string to enable the spelling correction,
'-a' before the query string to enable the automatic "word-wise"
spelling correction, performed directly by the system,
without specifying any further parameters (automatic
correction will work only if no results are found by the
system for the given query),
the query string without any flags to retrieve exact matches only.
A combination of the previous flags can be used together. For phonetic
and spelling corrections, the number of corrections to attempt can be
specified by adding the number (the edit distance) after the flag (e.g.,
-p2 foo&bar
means to try to correct (phonetic correction) two times; this does
not mean that the final query string (after the correction) will have
edit distance of 2 from the initial query string, but it means that
it will have an edit distance of at least 2, in fact, suppose that
no corrections are available at distance 1, so the system automatically
increases the edit-distance to 2 and re-try: after one single attempt
of correction, in this example, the overall edit-distance will be 2,
but in the input string we specified to try to correct 2 times, hence
another correction attempt will be made and the resulting edit distance
will be >=2).
The query string can have AND, OR or NOT operations (use the symbols '&',
'|', '!' respectively, spaces are interpreted as AND queries).
The system recognizes parenthesis, which can be used in any query string
to specify the precedences.

Insert a query or '-q' to exit:
space & jam
14 results found in 22.6091 ms
First 10 results:
- {"Space Jam": {"Title":"Space Jam","Release date":"1996-11-10T00:00:00Z","Box office revenue":
- {"Dr. No": {"Title":"Dr. No","Release date":"1962-10-05T00:00:00Z","Box office revenue":"59600
- {"The Sandlot 2": {"Title":"The Sandlot 2","Release date":"2005-05-03T00:00:00Z","Box office r
- {"Box Office Bunny": {"Title":"Box Office Bunny","Release date":"1990-01-01T00:00:00Z","Box of
- {"Stony Island": {"Title":"Stony Island","Release date":"1978-01-01T00:00:00Z","Box office rev
- {"Rock School": {"Title":"Rock School","Release date":"2005-06-03T00:00:00Z","Box office reven
- {"Robotech: The Shadow Chronicles": {"Title":"Robotech: The Shadow Chronicles","Release date":
- {"The Day After Tomorrow": {"Title":"The Day After Tomorrow","Release date":"2004-05-17T00:00:
- {"Quantum Apocalypse": {"Title":"Quantum Apocalypse","Release date":"2010-02-24T00:00:00Z","Bo
- {"The Time Machine": {"Title":"The Time Machine","Release date":"2002-03-04T00:00:00Z","Box of
4 results not showed yet. Do you want to see more results? [y/n]: n

Insert a query or '-q' to exit:
!space & jam | (bunny & garden)
231 results found in 48.9478 ms
First 10 results:
- {"Camp Rock": {"Title":"Camp Rock","Release date":"2008-06-20T00:00:00Z","Box office revenue":
- {"My Super Psycho Sweet 16: Part 2": {"Title":"My Super Psycho Sweet 16: Part 2","Release date
- {"They Call Her Cleopatra Wong": {"Title":"They Call Her Cleopatra Wong","Release date":"1978-
- {"Log Jammed": {"Title":"Log Jammed","Release date":null,"Box office revenue":null,"Run time":
- {"Camp Rock 2: The Final Jam": {"Title":"Camp Rock 2: The Final Jam","Release date":"2010-09-0
- {"Lumber Jack-Rabbit": {"Title":"Lumber Jack-Rabbit","Release date":"1954-01-01T00:00:00Z","Bo
- {"The Cube": {"Title":"The Cube","Release date":"1969-01-01T00:00:00Z","Box office revenue":nu
- {"Going Postal": {"Title":"Going Postal","Release date":"2010-01-01T00:00:00Z","Box office rev
- {"Alice in Wonderland": {"Title":"Alice in Wonderland","Release date":"1985-12-09T00:00:00Z","
- {"Ready to Run": {"Title":"Ready to Run","Release date":"2000-07-14T00:00:00Z","Box office rev
221 results not showed yet. Do you want to see more results? [y/n]: n

Insert a query or '-q' to exit:
!space & ( jam | bunny) & garden
16 results found in 5.8014 ms
First 10 results:
        - {"Gnomeo and Juliet": {"Title":"Gnomeo and Juliet","Release date":"2011-01-23T00:00:00Z","Box 
        - {"Lumber Jack-Rabbit": {"Title":"Lumber Jack-Rabbit","Release date":"1954-01-01T00:00:00Z","Bo
        - {"Alice in Wonderland": {"Title":"Alice in Wonderland","Release date":"1999-02-28T00:00:00Z","
        - {"A Ferret Called Mickey": {"Title":"A Ferret Called Mickey","Release date":"2003-10-18T00:00:
        - {"Band of the Hand": {"Title":"Band of the Hand","Release date":"1986-04-11T00:00:00Z","Box of
        - {"Winnie the Pooh: Seasons of Giving": {"Title":"Winnie the Pooh: Seasons of Giving","Release 
        - {"Beanstalk Bunny": {"Title":"Beanstalk Bunny","Release date":"1955-02-12T00:00:00Z","Box offi
        - {"Bunny Lake Is Missing": {"Title":"Bunny Lake Is Missing","Release date":"1965-10-03T00:00:00
        - {"End of the World": {"Title":"End of the World","Release date":"1931-01-23T00:00:00Z","Box of
        - {"Wet Hare": {"Title":"Wet Hare","Release date":null,"Box office revenue":null,"Run time":null
6 results not showed yet. Do you want to see more results? [y/n]: y
        - {"Alice in Wonderland": {"Title":"Alice in Wonderland","Release date":"1985-12-09T00:00:00Z","
        - {"Here Comes Peter Cottontail": {"Title":"Here Comes Peter Cottontail","Release date":"1971-01
        - {"A Lucky Dog": {"Title":"A Lucky Dog","Release date":"1921-10-01T00:00:00Z","Box office reven
        - {"His Hare-Raising Tale": {"Title":"His Hare-Raising Tale","Release date":"1951-08-11T00:00:00
        - {"The Unfaithful Wife": {"Title":"The Unfaithful Wife","Release date":"1969-01-01T00:00:00Z","
        - {"John and Yoko: A Love Story": {"Title":"John and Yoko: A Love Story","Release date":"1985-12

Insert a query or '-q' to exit:
sp*e jam
67 results found in 51.3878 ms
First 10 results:
        - {"Robotech: The Shadow Chronicles": {"Title":"Robotech: The Shadow Chronicles","Release date":
        - {"Runaway Train": {"Title":"Runaway Train","Release date":"1985-11-15T00:00:00Z","Box office r
        - {"Babe: Pig in the City": {"Title":"Babe: Pig in the City","Release date":"1998-11-25T00:00:00
        - {"Dust Devil: The Final Cut": {"Title":"Dust Devil: The Final Cut","Release date":"1992-01-01T
        - {"Quantum Apocalypse": {"Title":"Quantum Apocalypse","Release date":"2010-02-24T00:00:00Z","Bo
        - {"The Kingdom": {"Title":"The Kingdom","Release date":"2007-08-22T00:00:00Z","Box office reven
        - {"Tintin and the Lake of Sharks": {"Title":"Tintin and the Lake of Sharks","Release date":"197
        - {"Book Revue": {"Title":"Book Revue","Release date":"1946-01-01T00:00:00Z","Box office revenue
        - {"Dragonfly": {"Title":"Dragonfly","Release date":"2002-02-18T00:00:00Z","Box office revenue":
        - {"They Call Her Cleopatra Wong": {"Title":"They Call Her Cleopatra Wong","Release date":"1978-
57 results not showed yet. Do you want to see more results? [y/n]: y
        - {"Dr. No": {"Title":"Dr. No","Release date":"1962-10-05T00:00:00Z","Box office revenue":"59600
        - {"Speed 2: Cruise Control": {"Title":"Speed 2: Cruise Control","Release date":"1997-06-13T00:0
        - {"The Day After Tomorrow": {"Title":"The Day After Tomorrow","Release date":"2004-05-17T00:00:
        - {"The Sandlot 2": {"Title":"The Sandlot 2","Release date":"2005-05-03T00:00:00Z","Box office r
        - {"I â?¥ Huckabees": {"Title":"I â?¥ Huckabees","Release date":"2004-09-10T00:00:00Z","Box offi
        - {"Band of the Hand": {"Title":"Band of the Hand","Release date":"1986-04-11T00:00:00Z","Box of
        - {"The Peacemaker": {"Title":"The Peacemaker","Release date":"1997-09-26T00:00:00Z","Box office
        - {"How to Eat Fried Worms": {"Title":"How to Eat Fried Worms","Release date":"2006-08-25T00:00:
        - {"High Time": {"Title":"High Time","Release date":"1960-09-16T00:00:00Z","Box office revenue":
        - {"John and Yoko: A Love Story": {"Title":"John and Yoko: A Love Story","Release date":"1985-12
47 results not showed yet. Do you want to see more results? [y/n]: y
        - {"Cosmopolis": {"Title":"Cosmopolis","Release date":"2012-05-25T00:00:00Z","Box office revenue
        - {"It Came from Beneath the Sea": {"Title":"It Came from Beneath the Sea","Release date":"1955-
        - {"Deliverance": {"Title":"Deliverance","Release date":"1972-07-30T00:00:00Z","Box office reven
        - {"Blade II": {"Title":"Blade II","Release date":"2002-03-21T00:00:00Z","Box office revenue":"1
        - {"Water for Elephants": {"Title":"Water for Elephants","Release date":"2011-04-22T00:00:00Z","
        - {"Wild Wild West": {"Title":"Wild Wild West","Release date":"1999-06-30T00:00:00Z","Box office
        - {"Manhattan Baby": {"Title":"Manhattan Baby","Release date":"1982-08-12T00:00:00Z","Box office
        - {"30 Days of Night": {"Title":"30 Days of Night","Release date":"2007-10-16T00:00:00Z","Box of
        - {"2000 AD": {"Title":"2000 AD","Release date":"2000-01-01T00:00:00Z","Box office revenue":null
        - {"Gunless": {"Title":"Gunless","Release date":"2010-04-30T00:00:00Z","Box office revenue":null
37 results not showed yet. Do you want to see more results? [y/n]: y
        - {"Dark Night of the Scarecrow": {"Title":"Dark Night of the Scarecrow","Release date":"1981-01
        - {"Saroja": {"Title":"Saroja","Release date":"2008-09-05T00:00:00Z","Box office revenue":null,"
        - {"April Fools": {"Title":"April Fools","Release date":"2007-09-11T00:00:00Z","Box office reven
        - {"Cloudy With A Chance of Meatballs": {"Title":"Cloudy With A Chance of Meatballs","Release da
        - {"Space Jam": {"Title":"Space Jam","Release date":"1996-11-10T00:00:00Z","Box office revenue":
        - {"The Sentinel": {"Title":"The Sentinel","Release date":"2006-04-19T00:00:00Z","Box office rev
        - {"Hollywood Outlaw Movie": {"Title":"Hollywood Outlaw Movie","Release date":null,"Box office r
        - {"Memphis Belle": {"Title":"Memphis Belle","Release date":"1990-01-01T00:00:00Z","Box office r
        - {"Full Metal Jacket": {"Title":"Full Metal Jacket","Release date":"1987-06-17T00:00:00Z","Box 
        - {"Assault on Precinct 13": {"Title":"Assault on Precinct 13","Release date":"2005-01-19T00:00:
27 results not showed yet. Do you want to see more results? [y/n]: n

Insert a query or '-q' to exit:
-q

>
```