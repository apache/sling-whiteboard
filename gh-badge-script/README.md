[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling GitHub Badge Script

A simple utility to automatically update the badges in GitHub's README.md files.

## Use

All repositories:

./add-badges.sh [SLING_DIR]

Single repository:

./add-badges.sh [SLING_DIR] [REPO_NAME]

## Dependencies

This script depends on the following utilities:

 - xpath
 - [grip](https://github.com/joeyespo/grip)

## Prerequisites

 1. Use the repo tool to extract all of the repositories in the [sling aggregator](https://github.com/apache/sling-aggregator)
 2. Ensure you have SSH based access enabled to GitHub
 3. Ensure all repository workspaces are in a clean state
