#!/bin/bash

set -e

mvn clean package
docker build -f docker/Dockerfile -t yti-codelist-content-intake-service .
