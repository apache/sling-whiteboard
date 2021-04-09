<a name="SlingPost"></a>

## SlingPost
A class for interacting with the Sling Post Servlet.

**Kind**: global class  

* [SlingPost](#SlingPost)
    * [new SlingPost(config)](#new_SlingPost_new)
    * _instance_
        * [.repositoryPath(file)](#SlingPost+repositoryPath)
        * [.copy(fromPath, toPath)](#SlingPost+copy)
        * [.delete(path)](#SlingPost+delete)
        * [.importContent(content, path, [contentType], [replace], [replaceProperties])](#SlingPost+importContent)
        * [.importFile(file, path, contentType, replace, replaceProperties)](#SlingPost+importFile)
        * [.move(fromPath, toPath)](#SlingPost+move)
        * [.post(path, params)](#SlingPost+post)
        * [.uploadFile(file, [path], [params])](#SlingPost+uploadFile)
    * _static_
        * [.addAutomaticProperties(params)](#SlingPost.addAutomaticProperties)

<a name="new_SlingPost_new"></a>

### new SlingPost(config)
Construct a new SlingPost instance


| Param | Type | Description |
| --- | --- | --- |
| config | <code>\*</code> |  |
| [config.url] | <code>string</code> | the url of the Apache Sling instance to connect to |
| [config.username] | <code>string</code> | the username to authenticate with Apache Sling |
| [config.password] | <code>string</code> | the password to authenticate with Apache Sling |
| [config.base] | <code>string</code> | the base directory to use when creating file paths |
| [config.level] | <code>string</code> | the logging level for configuring the logger |

<a name="SlingPost+repositoryPath"></a>

### slingPost.repositoryPath(file)
Calculates the Sling repository path for a file based on the configured base path

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  

| Param | Type | Description |
| --- | --- | --- |
| file | <code>string</code> | the file for which to calculate the repository path |

<a name="SlingPost+copy"></a>

### slingPost.copy(fromPath, toPath)
Copies the item addressed by the fromPath parameter to a new location indicated by the toPath parameter

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  
**See**: https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#copying-content-1  

| Param | Type | Description |
| --- | --- | --- |
| fromPath | <code>string</code> | the absolute path to the item to move |
| toPath | <code>string</code> | the absolute or relative path to which the resource is copied. If the path is relative it is assumed to be below the same parent as the request resource. If it is terminated with a / character the request resource is copied to an item of the same name under the destination path. |

<a name="SlingPost+delete"></a>

### slingPost.delete(path)
Remove existing content

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  
**See**: https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#content-removal-1  

| Param | Type | Description |
| --- | --- | --- |
| path | <code>string</code> | The absolute path of the item to delete |

<a name="SlingPost+importContent"></a>

### slingPost.importContent(content, path, [contentType], [replace], [replaceProperties])
Import content into the Sling repository from a string.

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  
**See**: https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#importing-content-structures-1  

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| content | <code>string</code> |  | Specifies content string to import. The format of the import content is the same as is used by the jcr.contentloader bundle. |
| path | <code>string</code> |  | The absolute path of the parent item under which to import the content |
| [contentType] | <code>string</code> | <code>&quot;json&quot;</code> | Specifies the type of content being imported. Possible values are: xml, jcr.xml, json, jar, zip |
| [replace] | <code>boolean</code> | <code>true</code> | Specifies whether the import should replace any existing nodes at the same path. Note: When true, the existing nodes will be deleted and a new node is created in the same place. |
| [replaceProperties] | <code>boolean</code> | <code>true</code> | Specifies whether the import should replace properties if they already exist. |

<a name="SlingPost+importFile"></a>

### slingPost.importFile(file, path, contentType, replace, replaceProperties)
Import content into the Sling repository from a file.

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  
**See**: https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#importing-content-structures-1  

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| file | <code>string</code> |  | Specifies a file uploaded for import. The format of the import content is the same as is used by the jcr.contentloader bundle. |
| path | <code>string</code> |  | The absolute path of the parent item under which to import the content. If not specified, the base path will be used to calculate the repository path. |
| contentType | <code>string</code> | <code>&quot;json&quot;</code> | Specifies the type of content being imported. Possible values are: xml, jcr.xml, json, jar, zip |
| replace | <code>boolean</code> | <code>true</code> | Specifies whether the import should replace any existing nodes at the same path. Note: When true, the existing nodes will be deleted and a new node is created in the same place. |
| replaceProperties | <code>boolean</code> | <code>true</code> | Specifies whether the import should replace properties if they already exist. |

<a name="SlingPost+move"></a>

### slingPost.move(fromPath, toPath)
Moves the item addressed by the fromPath to a new location indicated by the toPath parameter.

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  
**See**: https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#moving-content  

| Param | Type | Description |
| --- | --- | --- |
| fromPath | <code>string</code> | the absolute path to the item to move |
| toPath | <code>string</code> | the absolute or relative path to which the resource is moved. If the path is relative it is assumed to be below the same parent as the request resource. If it is terminated with a / character the request resource is moved to an item of the same name under the destination path. |

<a name="SlingPost+post"></a>

### slingPost.post(path, params)
Sends a POST request to the Apache Sling Post Servlet

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  

| Param | Type | Description |
| --- | --- | --- |
| path | <code>string</code> | the path to execute the command |
| params | <code>Object</code> | the paramters to send to the Apache Sling Post API |

<a name="SlingPost+uploadFile"></a>

### slingPost.uploadFile(file, [path], [params])
Upload a file into the Apache Sling repository

**Kind**: instance method of [<code>SlingPost</code>](#SlingPost)  
**See**: https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#file-uploads  

| Param | Type | Description |
| --- | --- | --- |
| file | <code>string</code> | The file or glob of files to upload into the Apache Sling repository |
| [path] | <code>string</code> | The path under which to upload the file. If not specified, the base path will be used to calculate the repository path. |
| [params] | <code>Object</code> | Additional parameters to send to the Apache Sling Post API |

<a name="SlingPost.addAutomaticProperties"></a>

### SlingPost.addAutomaticProperties(params)
Add the automatic properties into the params

**Kind**: static method of [<code>SlingPost</code>](#SlingPost)  
**See**: https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#automatic-property-values-last-modified-and-created-by  

| Param | Type |
| --- | --- |
| params | <code>Object</code> | 

