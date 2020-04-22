# Intro

`slingpackager` is a nodejs based command line tool alternative to the [Content Package Maven Plugin](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/vlt-mavenplugin.html) from Adobe or the
[Content Package Maven Pluging](https://wcm.io/tooling/maven/plugins/wcmio-content-package-maven-plugin/) from
[WCM.IO](https://wcm.io). It allows to work with content
packages intended for any [Apache Sling](https://sling.apache.org) based product using composum as its
package manager or Adobe AEM with the CRX package manager. 

It covers the following use cases:

- build a content package form a local folder
- upload a content package onto a server
- download a content package from server
- install a content package on a server
- list all installed packages on the server
- uninstall a content package from the server
- build a content package on the server
- delete a content package on the server

For an example project using the `slingpackager` please have a look at the [simple-sling-vue-example](https://github.com/peregrine-cms/simple-sling-vue-example) project.

## Installation

To install slingpackager globally use

```
npm install @peregrinecms/slingpackager -g
```

This will make the packager available as a command line tool on your system. If you'd like to install `slingpackager` to your project only (or to try it before you commit to a global install), in your project folder, use

```
npm install @peregrinecms/slingpackager
```

You can then execute

```
npx slingpackager
```

to run slingpackager in your project.

## Supported Commands

```
slingpackager <command>

Commands:
  slingpackager build <package>      build package on server
  slingpackager delete <package>     delete package on server
  slingpackager download <package>   download package from server
  slingpackager install <package>    install package on server
  slingpackager list                 list installed packages
  slingpackager package <folder>     create a package
  slingpackager test                 test package manager service connection
  slingpackager uninstall <package>  uninstall package on server
  slingpackager upload <package>     upload package to server

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --server, -s   server url                   [default: "http://localhost:8080"]
  --user, -u     server credentials in the form username:password
                                                        [default: "admin:admin"]
  --verbose, -v  turn on verbose output
```

### List

```
slingpackager list

list installed packages

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --server, -s   server url                   [default: "http://localhost:8080"]
  --user, -u     server credentials in the form username:password
                                                        [default: "admin:admin"]
  --verbose, -v  turn on verbose output
  ```

### Package

```
slingpackager package <folder>

create a package

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --verbose, -v  turn on verbose output
  --destination, -d  Package destination directory. Defaults to current
                     directory.
  --config, -c       Package configuration/properties. Package properties.xml
                     and package name are generated from this. If this option is
                     missing slingpackage will search for
                     slingpackager.config.js in the parent directories.
```

The ```<folder>``` should point to the parent folder of jcr_root and META_INF with your project's content.

Bellow is an example of minimal configuration required for package generation using ```package``` command.

```
module.exports = {
    "vault-properties": {
		"comment": "myapp - UI Apps",
		"entry": {
			"name": "ui.apps",
			"version": "1.0-SNAPSHOT",
			"group": "myapp"
		}
    }
}
```

This should be placed in the file named _slingpackager.config.js_ anywhere in the folder path passed to ```package``` command (we suggest project's root folder). Or path to configuration file can be specified via --config option. 

The above example will result in a package file named ui.apps-1.0-SNAPSHOT.zip with the fillowing properties.xml file inside it's META/vault folder.

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
  <comment>myapp - UI Apps</comment>
  <entry key="name">ui.apps</entry>
  <entry key="version">1.0-SNAPSHOT</entry>
  <entry key="group">themeclean-flex</entry>
  <entry key="createdBy">slingpackager</entry>
  <entry key="acHandling">IGNORE</entry>
  <entry key="allowIndexDefinitions">false</entry>
  <entry key="requiresRoot">false</entry>
  <entry key="path">/etc/packages/myapp/ui.apps-1.0-SNAPSHOT.zip</entry>
</properties>
```

### Upload

```
slingpackager upload <package>

upload package to server

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --server, -s   server url                   [default: "http://localhost:8080"]
  --user, -u     server credentials in the form username:password
                                                        [default: "admin:admin"]
  --verbose, -v  turn on verbose output
  --install, -i  install the package after it's uploaded
```

### Download

```
slingpackager download <package>

download package from server

Options:
  --version          Show version number                               [boolean]
  --help             Show help                                         [boolean]
  --server, -s       server url               [default: "http://localhost:8080"]
  --user, -u         server credentials in the form username:password
                                                        [default: "admin:admin"]
  --retry, -r        maximum number of service calls to attempt before failing
                                                                   [default: 10]
  --verbose, -v      turn on verbose output           [boolean] [default: false]
  --destination, -d  Package destination directory. Defaults to current
                     directory.
```

### Install

```
slingpackager install <package>

install package on server

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --server, -s   server url                   [default: "http://localhost:8080"]
  --user, -u     server credentials in the form username:password
                                                        [default: "admin:admin"]
  --verbose, -v  turn on verbose output
```

### Uninstall

```
slingpackager uninstall <package>

uninstall package on server

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --server, -s   server url                   [default: "http://localhost:8080"]
  --user, -u     server credentials in the form username:password
                                                        [default: "admin:admin"]
  --verbose, -v  turn on verbose output
```

### Build

```
slingpackager build <package>

build package on server

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --server, -s   server url                   [default: "http://localhost:8080"]
  --user, -u     server credentials in the form username:password
                                                        [default: "admin:admin"]
  --verbose, -v  turn on verbose output
```

### Delete

```
slingpackager delete <package>

delete package on server

Options:
  --version      Show version number                                   [boolean]
  --help         Show help                                             [boolean]
  --server, -s   server url                   [default: "http://localhost:8080"]
  --user, -u     server credentials in the form username:password
                                                        [default: "admin:admin"]
  --verbose, -v  turn on verbose output
```

### Examples

#### Create a package.
```
slingpackager package /pathToMyProject/pathToPackageContent
```

#### Upload package to server.
```
slingpackager upload ui.apps-1.0-SNAPSHOT.zip
```

#### Upload package to server and install it.
```
slingpackager upload ui.apps-1.0-SNAPSHOT.zip -i
```

#### Upload package to local AEM Author on port 4502 as user admin:somePassword.
```
slingpackager upload ui.apps-1.0-SNAPSHOT.zip -u admin:somePassword -s http://localhost:4502
```

#### List packages on the server.
```
slingpackager list
name=ui.apps group=themeclean-flex version=1.0-SNAPSHOT path=/themeclean-flex/ui.apps-1.0-SNAPSHOT.zip
name=ui.apps group=themeclean version=1.0-SNAPSHOT path=/themeclean/ui.apps-1.0-SNAPSHOT.zip
name=example-vue.ui.apps group=com.peregrine-cms.example version=1.0-SNAPSHOT path=/com.peregrine-cms.example/example-vue.ui.apps-1.0-SNAPSHOT.zip
name=admin.sling.ui.apps group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/admin.sling.ui.apps-1.0-SNAPSHOT.zip
name=admin.ui.apps group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/admin.ui.apps-1.0-SNAPSHOT.zip
name=admin.ui.materialize group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/admin.ui.materialize-1.0-SNAPSHOT.zip
name=base.ui.apps group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/base.ui.apps-1.0-SNAPSHOT.zip
name=external group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/external-1.0-SNAPSHOT.zip
name=felib.ui.apps group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/felib.ui.apps-1.0-SNAPSHOT.zip
name=node-js.ui.apps group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/node-js.ui.apps-1.0-SNAPSHOT.zip
name=node-js.ui.apps.script group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/node-js.ui.apps.script-1.0-SNAPSHOT.zip
name=pagerender-vue.ui.apps group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/pagerender-vue.ui.apps-1.0-SNAPSHOT.zip
name=replication.ui.apps group=com.peregrine-cms version=1.0-SNAPSHOT path=/com.peregrine-cms/replication.ui.apps-1.0-SNAPSHOT.zip
```

#### List packages on local AEM Author on port 4502.
```
slingpackager list -s http://localhost:4502
name=we.retail.config group=adobe/aem6 version=4.0.0 path=we.retail.config-4.0.0.zip
name=aem-sample-replication-config group=adobe/aem6/sample version=0.0.2 path=aem-sample-replication-config-0.0.2.zip
name=we.retail.commons.content group=adobe/aem6/sample version=4.0.0 path=we.retail.commons.content-4.0.0.zip
name=we.retail.community.apps group=adobe/aem6/sample version=1.11.84 path=we.retail.community.apps-1.11.84.zip
name=we.retail.community.content group=adobe/aem6/sample version=1.11.84 path=we.retail.community.content-1.11.84.zip
name=we.retail.community.enablement.author group=adobe/aem6/sample version=1.11.87 path=we.retail.community.enablement.author-1.11.87.zip
name=we.retail.community.enablement.common group=adobe/aem6/sample version=1.11.84 path=we.retail.community.enablement.common-1.11.84.zip
name=we.retail.ui.apps group=adobe/aem6/sample version=4.0.0 path=we.retail.ui.apps-4.0.0.zip
...
```

#### Download package.
```
slingpackager download /themeclean-flex/ui.apps-1.0-SNAPSHOT.zip
```

#### Install uploaded package.
```
slingpackager install /themeclean-flex/ui.apps-1.0-SNAPSHOT.zip
```

#### Uninstall package.
```
slingpackager uninstall /themeclean-flex/ui.apps-1.0-SNAPSHOT.zip
```

#### Build package.
```
slingpackager build /themeclean-flex/ui.apps-1.0-SNAPSHOT.zip
```

#### Delete package.
```
slingpackager delete /themeclean-flex/ui.apps-1.0-SNAPSHOT.zip
```