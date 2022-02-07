#!/usr/bin/env python3

# This Python script can be used to test the Java application,
# launching tests multiple times with different environment property values

import subprocess
import re

PATH_TO_PROJECT_PROPERTIES = "src/main/resources/properties.default.env"

# Test the Java application with all possible stemmers
STEMMER_PROPERTY_NAME = "app.stemmer"
REGEX_STEMMER_PROPERTY = "[ \\t]*" + STEMMER_PROPERTY_NAME + "[ \\t]*[=][ \\t]*([\\w]+)[ \\t]*"
GROUP_WITH_PROP_VALUE = 1
with open(PATH_TO_PROJECT_PROPERTIES, "r") as env_file:
    stemmer_initial_value = re.search(REGEX_STEMMER_PROPERTY, env_file.read()).group(GROUP_WITH_PROP_VALUE)


def set_stemmer_in_env_file(stemmer_):
    with open(PATH_TO_PROJECT_PROPERTIES, "r") as env_file_:
        file_content = env_file_.read()
    file_content = re.sub(REGEX_STEMMER_PROPERTY, STEMMER_PROPERTY_NAME + " = " + stemmer_, file_content)
    with open(PATH_TO_PROJECT_PROPERTIES, "w") as env_file_:
        env_file_.seek(0)
        env_file_.truncate()
        env_file_.write(file_content)


def is_process_still_running(process):
    return process.poll() is None  # .poll() returns None while the process is still running


errors = {}

for stemmer in ["null", "Porter"]:
    set_stemmer_in_env_file(stemmer)
    mvn_test_process = subprocess.Popen(["mvn", "test"],
                                        stdout=subprocess.PIPE,
                                        stderr=subprocess.PIPE,
                                        shell=True)
    errors_with_this_stemmer = ""
    while is_process_still_running(mvn_test_process):
        ENCODING = "utf-8"
        line = mvn_test_process.stdout.readline().decode(ENCODING)
        if line.startswith("[ERROR]"):
            errors_with_this_stemmer += line
        print("{" + STEMMER_PROPERTY_NAME + "=" + stemmer + "}\t" + line, end='')
    if errors_with_this_stemmer != "":
        errors[stemmer] = errors_with_this_stemmer

set_stemmer_in_env_file(stemmer_initial_value)

if len(errors) > 0:
    print("\n\n\n" +
          "######################################################################" +
          "################################ FAIL ################################" +
          "######################################################################\n")
    for stemmer, error in errors.items():
        print("\nWith " + STEMMER_PROPERTY_NAME + " = " + stemmer + "\n" + error)
