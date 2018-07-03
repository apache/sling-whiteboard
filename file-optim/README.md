[<img src="http://sling.apache.org/res/logos/sling.png" align="center"/>](http://sling.apache.org)

[![Build Status](https://img.shields.io/jenkins/s/https/builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-file-optim-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-file-optim-1.8) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-sling-org-apache-sling-file-optim-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-sling-org-apache-sling-file-optim-1.8) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.file.optim/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.file.optim%22)

# Apache Sling File Optimization

Bundle for optimizing files stored in the Apache Sling repository. Includes a plugin architecture for providing file optimizers and hooks to automatically and manually optimize files.

The file optimizer includes the ability to revert the optimized file either using the Sling Post Operation or via the API. Resources can be excluded from optimization by setting the `optim:disabled` attribute to `true`.

This module is part of the [Apache Sling](https://sling.apache.org) project.

## Using the File Optimization Library

There are four different methods for interacting with the File Optimizer: using the Event Handler, interacting with the Servlets / Post Operations, using the Filter or invoking the API directly. 

### Event Handler

The File Optimizer Event Handler can be used to automatically optimize file resources when Sling Events occur. The File Optimizer Event Handler is not enabled by default, to enable it, you will need to enable the Event Handler with an OSGi Config like:

    # Example Event Filter
    event.filter=(&(resourceType=sling:File)(|(path=*.png)(path=*.jpg)))
    # Example Event Topic
    event.topic=org/apache/sling/api/resource/Resource/CHANGED
    
### Servlet / Post Operations

There are four servlets / Sling Post Servlet operations for interacting with the File Optimization API.

#### OptimizeFileOperation

This operation will optimize a File resource. Example usage:

    curl -d ":operation=fileoptim:optimize" -X POST http://localhost:8080/content/afile.jpg

#### RestoreOriginalOperation

This operation will restore the original contents of an optimized File resource. Example usage:

    curl -d ":operation=fileoptim:restore" -X POST http://localhost:8080/content/afile.jpg

#### FileOptimizerData

This servlet will return the JSON data for the results of an optimization operation  if it were run against the specified resource. Example usage:

    curl http://localhost:8080/system/fileoptim.json?path=/content/afile.jpg
    
Example response:

    {
        "algorithm": "Apache Sling JPEG File Optimizer",
        "originalSize": 1000,
        "optimizedSize", 500,
        "optimized", false,
        "preview", "/system/fileoptim/preview?path=/content/afile.jpg",
        "savings", 0.5
    }

#### FileOptimizerPreview

This servlet will return the optimized binary as if the File Optimizer were run against the specified resource. Example usage:

    curl http://localhost:8080/system/fileoptim/preview?path=/content/afile.jpg

### Filter

The File Optimizer Filter can be used to automatically optimize file resources when serving the content. The File Optimizer Filter is not enabled by default, to enable it, you will need to enable the Filter with an OSGi Config like:

    # Example Filter Scope
    sling.filter.scope=REQUEST

### API

The File Optimizer service can be retrieved by reference, for example:

    @Reference
    private FileOptimizerService fileOptimSvc;
    
    public void optimizeFile(Resource fileResource) {
        fileOptimSvc.optimizeFile(fileResource, true);
    }
    
Additionally, there are two Sling models for discovering the optimization information of resources.

 - *org.apache.sling.fileoptim.models.OptimizedFile* - Allows for retrieval of the data from a file which has been optimized
 - *org.apache.sling.fileoptim.models.OptimizeResource* - Allows for determining if a resource can be optimized and what the results would be if it were

## Defining a File Optimizer

File optimizers are used by the library to optimize resources based on the file mime type. Each File Optimizer should implement the [FileOptimizer](src/main/java/org/apache/sling/fileoptim/FileOptimizer.java) interface, setting the *mime.type* property to the MimeTypes for which the optimizer is applicable. The [Service Ranking](https://osgi.org/javadoc/r2/org/osgi/framework/Constants.html#SERVICE_RANKING) property can be used to override the default File Optimizers.

    @Component(service = FileOptimizer.class, property = { FileOptimizer.MIME_TYPE + "=image/jpeg" })
    public class DevNullFileOptimizer implements FileOptimizer {

        private static final Logger log = LoggerFactory.getLogger(DevNullFileOptimizer.class);

        @Override
        public byte[] optimizeFile(byte[] original, String metaType) {
            // TODO: Actually do something with the file contents here and return the optimized file
            return new byte[0];
        }

        @Override
        public String getName() {
            return "/dev/null File Optimizer";
        }
    }
