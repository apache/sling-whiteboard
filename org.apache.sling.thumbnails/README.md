[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

 [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Thumbnails

Generate and transform thumbnails from file-like Resources.

## Use

This library registers two servlets for generating thumbnails.

### Dynamic Transform Servlet

Generates and transforms thumbnails "on the fly" based on a request path, format and transformation JSON payload. Available for POST requests at the path `/bin/sling/thumbnails/transform` with the parameters `resource` (required) and `format` (optional), for example, given the following image at `/content/image/test.png`"

![docs/original.jpeg](docs/original.jpeg)<br/>
_Image credit [Markos Mant](https://unsplash.com/@markos_mant) from [Unsplash](https://unsplash.com/photos/0nKRq0IknHw)_

Sending a request like:

URL: http://localhost:8080/bin/sling/thumbnails/transform?resource=/content/image/test.png&format=jpeg

BODY:

```
[
    {
        "handlerType": "sling/thumbnails/transformers/crop",
        "properties": {
            "width": 300,
            "height": 300
        }
    },
    {
        "handlerType": "sling/thumbnails/transformers/colorize",
        "properties": {
            "red": 112,
            "green": 66,
            "blue": 20,
            "alpha": 0.4
        }
    }
]
```

Will result in the following thumbnail:

![docs/rendered.jpeg](docs/rendered.jpeg)

#### Saving Thumbnails

The Dynamic Transform Servlet can save the generated thumbnail for any Persistable resource type. To do so add the URL parameter `renditionName`, e.g.:

http://localhost:8080/bin/sling/thumbnails/transform?resource=/content/image/test.png&format=jpeg&renditionName=myrendition.jpeg

Once saved, the rendition can be access directly using the configured Rendition Path for the Resource Type or using the Transform Servlet. Note that the rendition will only be available on the resource the servlet is executed on.

### Transform Servlet

The second servlet uses [Sling Context Aware Configurations](https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration.html) to generate thumbnails based on pre-defined transformation pipelines. Note that the [RenderedResource](src/main/java/org/apache/sling/thumbnails/RenderedResource.java) model is useful for retrieving the available transformation pipelines and existing renditions for a particular resource.

Each available transformation can then be accessed in the form:

{/path/to/file.ext}.transform/{transformation-name}.{final-extension}

This for a request like:

/content/images/test.png.transform/my-transformation.jpeg

The servlet would:

1.  Get the resource `/content/images/test.png`
2.  Apply the transformation `my-transformation` (found in any CA Config)
3.  Convert the result to a JPEG

#### CA Config Structure

The structure for the transformations under the CA Config root (e.g. /conf/global/) should include files/transformations, as such:

```
/conf/global: {
  "jcr:primaryType": "sling:Folder",
  "files": {
    "jcr:primaryType": "sling:Folder",
    "transformations": {
      "jcr:primaryType": "sling:Folder",
      "transformation": {
        "jcr:primaryType": "nt:unstructured",
        "name": "transformation",
        "sling:resourceType": "sling/thumbnails/transformation",
        "handlers": {
          "jcr:primaryType": "nt:unstructured",
          "resize": {
            "jcr:primaryType": "nt:unstructured",
            "height": 200,
            "width": 200,
            "sling:resourceType": "sling/thumbnails/transformers/resize"
          }
        }
      }
    }
  }
}
```

#### Persistence

The Transform Servlet supports persisting renditions and using the persisted renditions instead of rendering the image on the fly. To persist renditions, you must configure the `persistableTypes` in the [Thumbnail Support Configuration](#Configuration)

The `persistableTypes` node type must also be in the `supportedTypes` list. The rendition will be persisted at the provided path as an `nt:file` node with the name provided when requesting the rendition.

## Installation

This library can be installed on Sling 11+ but does require the following libraries:

- org.apache.commons:commons-compress:1.20
- org.apache.commons:commons-math3:3.6.1
- org.apache.servicemix.bundles:org.apache.servicemix.bundles.poi:4.1.2_2
- org.apache.servicemix.bundles:org.apache.servicemix.bundles.xmlbeans:3.1.0_2

Add a service user `sling-thumbnails` with the following permissions:

- jcr:write,jcr:nodeTypeManagement,jcr:versionManagement on /content
- jcr:read on /

Add a service user mapping: `org.apache.sling.thumbnails:sling-commons-thumbnails=sling-thumbnails`

Note that this library generates [Sling Feature Models](https://sling.apache.org/documentation/development/feature-model.html) which can be used to install it easily with the Sling Feature Launcher:

- org.apache.sling:org.apache.sling.thumbnails:slingosgifeature:base:{RELEASE_VERSION}
- org.apache.sling:org.apache.sling.thumbnails:slingosgifeature:default:{RELEASE_VERSION}
- org.apache.sling:org.apache.sling.thumbnails:slingosgifeature:dependencies:{RELEASE_VERSION}

## Configuration

This module requires configuring the following pid:

```
PID = org.apache.sling.thumbnails.internal.ThumbnailSupportImpl
  errorResourcePath = /static/sling-cms/thumbnails/file.png
  errorSuffix = /sling-cms-thumbnail.png
  persistedTypes = []
  supportedTypes = [nt:file=jcr:content/jcr:mimeType]
```

The the values should be set as follows:

 - `errorResourcePath` - the path to a resource to transform if an error occurs instead of returning the default error page
 - `errorSuffix` - the transformation to call on the error resource if an error occurs instead of returning the default error page
 - `persistedTypes` - The types which support persistence of renditions in the format _resourceType=rendition-path_
 - `supportedTypes` - The types which support thumbnail generation and transformation in the format _resourceType=metdata-path_

**Note:** the _supportedTypes_ (and by extension _persistedTypes_) must be adaptable to from a Resource to an java.io.InputStream.

Generally, this configuration should be provided by the application including this functionality. See [ThumbnailSupportConfig](src/main/java/org/apache/sling/thumbnails/internal/ThumbnailSupportConfig.java) for more on the configuration values.

## Primary Concepts

There are two main concepts in this library:

- `ThumbnailProviders` - implement the interface [ThumbnailProvider](src/main/java/org/apache/sling/thumbnails/ThumbnailProvider.java) and are responsible for generating a thumbnail from a defined file node
- `TransformationHandler` - implement the class [TransformationHandler](src/main/java/org/apache/sling/thumbnails/TransformationHandler.java) and are responsible for executing a single transformation as part of a thumbnail transformation pipeline

Default Thumbnail Providers and Transformation Handlers are provided with this library and both interfaces are available for extension.

## Thumbnail Providers

Thumbnail Providers implement the `ThumbnailProvider` interface and are responsible for generating a thumbnail from a file resource. Each handler is expected to indicate whether or not it can handle the provided resource / mime type and will be evaluated in order of their Service Ranking with the `TikaFallbackProvider` having a service ranking of Integer.MIN_VALUE

The following are the included Thumbnail Providers:

### Image Thumbnail Provider

Directly uses the image as the thumbnail

_Implementation_: `org.apache.sling.thumbnails.internal.providers.ImageThumbnailProvider`

_Supported Type(s)_: All image types, except SVG

### PDF Thumbnail Provider

Generates a PDF thumbnail using PDFBox

_Implementation_: `org.apache.sling.thumbnails.internal.providers.PDFThumbnailProvider`

_Supported Type(s)_: PDF documents

### Slide Show Thumbnail Provider

Generates a thumbnail from PPTX / PPT documents using POI

_Implementation_: `org.apache.sling.thumbnails.internal.providers.SlideShowThumbnailProvider`

_Supported Type(s)_: PPTX / PPT documents

### Tika Fallback Thumbnail Provider

Generates a thumbnail using Apache Tika

_Implementation_: `org.apache.sling.thumbnails.internal.providers.TikaFallbackProvider`

_Supported Type(s)_: Any remaining document type

## Transformation Handlers

Transformation Handlers implement the `TransformationHandler` interface and are responsible for invoking tranformation effects on thumbnails. Each Transformation Handler is identified by a Handler Type which should correspond to a Sling Resource Type. Only one Transformation Handler is expected to be registered per resource type.

Most of the transformnation handlers use the [Thumbnailator](https://github.com/coobird/thumbnailator) library under the hood to perform the transformations.

The following are the included Transformation Handlers:

### Colorize Hander

Adds a color tint to an image.

_Implementation_: `org.apache.sling.thumbnails.internal.transformers.ColorizeHandler`

_Handler Type_: `sling/thumbnails/transformers/colorize`

_Parameters_

- red - the red color value (0-255)
- green - the green color value (0-255)
- blue - the blue color value (0-255)
- alpha - the level of transparency, with lower being more transparent (0.0 - 1.0)

### Crop Handler

Crops an image to the size specified. Note the width and height must be specified.

_Implementation_: `org.apache.sling.thumbnails.internal.transformers.ColorizeHandler`

_Handler Type_: `sling/thumbnails/transformers/colorize`

_Parameters_

- height - the height to constrain the crop (1+)
- width - the width to constrain the crop (1+)
- position - one of:
  - BOTTOM_CENTER
  - BOTTOM_LEFT
  - BOTTOM_RIGHT
  - CENTER
  - CENTER_LEFT
  - CENTER_RIGHT
  - TOP_CENTER
  - TOP_LEFT
  - TOP_RIGHT

### Greyscale Handler

Converts an image to greyscale. Note this will remove transparency.

_Implementation_: `org.apache.sling.thumbnails.internal.transformers.GreyscaleHandler`

_Handler Type_: `sling/thumbnails/transformers/greyscale`

_Parameters_

N/A

### Resize Handler

Resizes an image to the size specified. If only one dimension is specified the image will be sized to the other

_Implementation_: `org.apache.sling.thumbnails.internal.transformers.ResizeHandler`

_Handler Type_: `sling/thumbnails/transformers/resize`

_Parameters_

- height - the height to constrain the crop (1+)
- width - the width to constrain the crop (1+)
- keepAspectRatio - boolean, if false, the exact width and height will be used which will not preserve the aspect ratio of the original image.

### Rotate Handler

Rotates an image.

_Implementation_: `org.apache.sling.thumbnails.internal.transformers.RotateHandler`

_Handler Type_: `sling/thumbnails/transformers/rotate`

_Parameters_

- degrees - the number of degrees to rotate the image (any number)

### Scale Handler

Sets the scaling factor for the width and height of the image. If the scaling factor for the width and height are not equal, then the image will not preserve the aspect ratio of the original image.

Note: either both or the width+height must be set.

_Implementation_: `org.apache.sling.thumbnails.internal.transformers.ScaleHandler`

_Handler Type_: `sling/thumbnails/transformers/scale`

_Parameters_

- both - scale the thumbnail by the same factor for width and height (0+ - 1.0+)
- width - scale the thumbnail width (0+ - 1.0+)
- height - scale the thumbnail height (0+ - 1.0+)

### Transparency Handler

Makes an image transparent

_Implementation_: `org.apache.sling.thumbnails.internal.transformers.TransparencyHandler`

_Handler Type_: `sling/thumbnails/transformers/transparency`

_Parameters_

- alpha - the level of transparency, with lower being more transparent (0.0 - 1.0)
