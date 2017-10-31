#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

cd github/endpoints-framework-maven-plugin
./mvnw clean install cobertura:cobertura -B -U
