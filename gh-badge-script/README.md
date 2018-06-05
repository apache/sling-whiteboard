[<img src="http://sling.apache.org/res/logos/sling.png" align="center"/>](http://sling.apache.org)

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
