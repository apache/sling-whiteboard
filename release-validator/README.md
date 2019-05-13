# Release Validator

A docker container for validating Apache Sling releases using the release ID. 

This container will:

 1. Download / load the Sling Committer Public Keys
 1. Validate the code signatures for the release
 1. Build each progress in the release
 1. If any project in the release is in a bundle, start an Apache Sling Starter and install as a bundle

## Setup

This project assumes you have already [installed](https://docs.docker.com/install/) on your local system.

To build the container instance, you must first download the desired JDK. Download the RPM Linux 64 installer
into the project root directory, then run:

    docker build -t sling-check-release .

## Commands

To run the docker container run:

    docker run -e RELEASE_ID=[A_RELEASE_NUMBER] sling-check-release

To keep the docker container up for 10 minutes to ensure the bundle is installed and working, execute:

    docker run -e RELEASE_ID=[A_RELEASE_NUMBER] -e KEEP_RUNNING=true -P sling-check-release
    
## Environment Variables

The following environment variables are supported:

 - **KEEP_RUNNING** - If set to true, the Sling instace will be left running, default is false
 - **RELEASE_ID** - The ID of the release to validate, required
 - **RUN_TIMEOUT** - The amount of time for the Sling instance to be left running, default is 10m
