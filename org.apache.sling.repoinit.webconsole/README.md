[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling RepoInit WebConsole

Test and run [Sling RepoInit](https://sling.apache.org/documentation/bundles/repository-initialization.html) scripts from the web console.

## Not For Production Use

**Do not install in production systems.** This bundle is not for production use and is only meant to be used for testing and development. 

## Installation

Either download the latest version of this bundle from the [Sling Downloads page](https://sling.apache.org/downloads.cgi) or build from source by checking out the project from Git and building with Maven:

`mvn clean install sling:install`

Note installing from source requires Java 11 or later and Maven 3.x or later.

This project has been tested to work with Sling 11+.

## Configuration

To enable execute functionality of the web console, you must add a Apache Sling Login Admin Whitelist entry. Open the OSGi console to [/system/console/configMgr](http://localhost:8080/system/console/configMgr) and add an entry for `org.apache.sling.repoinit.webconsole` either to an existing configuration fragment or a new one.

![Configuring the Whitelist](docs/Configure-Whitelist.png)

## Use

To use the plugin, navigate to [/system/console/repoinit](http://localhost:8080/system/console/repoinit) and enter the script you want to run into the source text area. Select the `Evaluate` button to validate the script, if you want to execute the script, select the `Execute` checkbox.

Assuming the script is valid a JSON representation will be displayed in the `Parsed Statements` section and the corresponding Feature Model JSON will display in the `Feature Model JSON` section.

![Evaluating a RepoInit Statement](docs/Evaluate.png)

If your script is not valid, an error message will be displayed in the `Messages` section. 

![Example Error Message](docs/Error.png)

If you get the error message:

> Failed to apply statements [LoginException]: Bundle org.apache.sling.repoinit.webconsole is NOT whitelisted

You need to add the bundle to the login admin whitelist. See Configuration above.