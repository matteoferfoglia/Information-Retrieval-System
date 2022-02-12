#!/usr/bin/env python3

# This Python script can be used to test the Java application,
# launching tests multiple times with different environment property values

import subprocess
import re

BENCHMARK = True  # true if benchmarks must be performed too

###############################################
#        DO NOT MODIFY UNDER THIS LINE        #
###############################################


#################################################
# Global variables

PATH_TO_PROJECT_PROPERTIES = "src/main/resources/properties.default.env"

# Test the Java application with different configurations

EXCLUDE_STOP_WORDS_PROPERTY_NAME = "app.exclude_stop_words"
RANK_QUERY_RESULTS_PROPERTY_NAME = "app.rank_query_results"
USE_WF_IDF_PROPERTY_NAME = "app.use_wf_idf"
STEMMER_PROPERTY_NAME = "app.stemmer"

AVAILABLE_STEMMERS = ["null", "Porter"]


#################################################
# Begin of procedures and scripts


def regex_for_property(property_name):
    return "[ \\t]*" + property_name + "[ \\t]*[=][ \\t]*([\\w]+)[ \\t]*"


def get_property_value(property_name):
    with open(PATH_TO_PROJECT_PROPERTIES, "r") as env_file:
        regex = regex_for_property(property_name)
        group_with_prop_value = 1
        return re.search(regex, env_file.read()).group(group_with_prop_value)


exclude_stop_words_initial_value = get_property_value(EXCLUDE_STOP_WORDS_PROPERTY_NAME)
rank_query_results_initial_value = get_property_value(RANK_QUERY_RESULTS_PROPERTY_NAME)
use_wf_idf_initial_value = get_property_value(USE_WF_IDF_PROPERTY_NAME)
stemmer_initial_value = get_property_value(STEMMER_PROPERTY_NAME)


def set_property_in_env_file(prop_name, prop_value):
    with open(PATH_TO_PROJECT_PROPERTIES, "r") as env_file_:
        file_content = env_file_.read()
    file_content = re.sub(regex_for_property(prop_name), prop_name + " = " + prop_value, file_content)
    with open(PATH_TO_PROJECT_PROPERTIES, "w") as env_file_:
        env_file_.seek(0)
        env_file_.truncate()
        env_file_.write(file_content)


def is_process_still_running(process):
    return process.poll() is None  # .poll() returns None while the process is still running


errors = {}

booleans = ["true", "false"]
for exclude_stop_words in booleans:
    for rank_query_results in booleans:
        for use_wf_idf in booleans:
            for stemmer in AVAILABLE_STEMMERS:

                set_property_in_env_file(EXCLUDE_STOP_WORDS_PROPERTY_NAME, exclude_stop_words)
                set_property_in_env_file(RANK_QUERY_RESULTS_PROPERTY_NAME, rank_query_results)
                set_property_in_env_file(USE_WF_IDF_PROPERTY_NAME, use_wf_idf)
                set_property_in_env_file(STEMMER_PROPERTY_NAME, stemmer)

                current_config = "{" + EXCLUDE_STOP_WORDS_PROPERTY_NAME + "=" + exclude_stop_words + ", " + \
                                 RANK_QUERY_RESULTS_PROPERTY_NAME + "=" + rank_query_results + ", " + \
                                 USE_WF_IDF_PROPERTY_NAME + "=" + use_wf_idf + ", " + \
                                 STEMMER_PROPERTY_NAME + "=" + stemmer + "}"

                mvn_commands = ["clean", "test"]
                if BENCHMARK:
                    mvn_commands = mvn_commands + ["exec:java@benchmark"]

                mvn_test_process = subprocess.Popen(["mvn"] + mvn_commands,
                                                    stdout=subprocess.PIPE,
                                                    stderr=subprocess.PIPE,
                                                    shell=True)
                errors_with_this_config = ""
                while is_process_still_running(mvn_test_process):
                    ENCODING = "utf-8"
                    try:
                        line = mvn_test_process.stdout.readline().decode(ENCODING)
                    except Exception:
                        line = ""   # ignore decoding errors
                    if line.startswith("[ERROR]") or line.startswith("[WARNING]") or line.startswith("java"):
                        errors_with_this_config += line
                    print(current_config + " \t" + line, end='')

                if errors_with_this_config != "":
                    errors[current_config] = errors_with_this_config

set_property_in_env_file(EXCLUDE_STOP_WORDS_PROPERTY_NAME, exclude_stop_words_initial_value)
set_property_in_env_file(RANK_QUERY_RESULTS_PROPERTY_NAME, rank_query_results_initial_value)
set_property_in_env_file(USE_WF_IDF_PROPERTY_NAME, use_wf_idf_initial_value)
set_property_in_env_file(STEMMER_PROPERTY_NAME, stemmer_initial_value)

if len(errors) > 0:
    print("\n\n\n" +
          "######################################################################" +
          "################################ FAIL ################################" +
          "######################################################################\n")
    for config, errors_ in errors.items():
        print("\nWith " + config + "\n")
        for error in errors_:
            print("\t" + error + "\n")
        print("\n\n")
