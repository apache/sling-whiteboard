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

## Commands

Generating a release vote email

    docker run --env-file=./docker-env apache/sling-cli release prepare-email $STAGING_REPOSITORY_ID
    
Generating a release vote result email

    docker run --env-file=./docker-env apache/sling-cli release tally-votes $STAGING_REPOSITORY_ID
    
## Assumptions

This tool assumes that the name of the staging repository matches the one of the version in Jira. For instance, the
staging repositories are usually named _Apache Sling Foo 1.2.0_. It is then expected that the Jira version is
named _Foo 1.2.0_. Otherwise the link between the staging repository and the Jira release can not be found.

It is allowed for staging repository names to have an _RC_ suffix, which may include a number, so that _RC_, _RC1_, _RC25_ are
all valid suffixes.  