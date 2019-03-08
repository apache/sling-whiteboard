# Apache Sling Engine CLI tool

This module is part of the [Apache Sling](https://sling.apache.org) project.

This module provides a command-line tool which automates various Sling development tasks. The tool is packaged
as a docker image.

## Configuration

To make various credentials and configurations available to the docker image it is recommended to use a docker env file.
A sample file is stored at `docker-env.sample`. Copy this file to `docker-env` and fill in your own information.

## Launching

The image is built using `mvn package`. Afterwards it may be run with

    docker run --env-file=./docker-env apache/sling-cli
    
This invocation produces a list of available subcommands.

Currently the only implemented command is generating the release vote email, for instance

    docker run --env-file=./docker-env apache/sling-cli release prepare-email $STAGING_REPOSITORY_ID