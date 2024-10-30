# Native Compilation for Sling-Mini

This subdirectory contains the resources needed to natively compile sling-mini.

## Building the Atomos-based Jar

To build, ensure that sling-mini is built first so that its aggregated features are available in the Maven cache.

Then

```
mvn install
```

This augments the sling-mini feature model with additional native image metadata and produces an flat-classpath sling-mini jar
in atomos-config/app.substrate.jar which can be launched with the feature launcher.

### Launching

To launch the atomos-based jar, run:

```
launch-offline.sh
```

By default the process will convert Markdown (`.md`) files found in `/tmp/docs` into HTML files written to `/tmp/offliner`


## Building the Native Image

After building the Atomos-based Jar, this can be used as input to the native compilation. This is be done with Graal native-image, so ensure GraalVM is installed.

To build:

```
./build-nativeoffline.sh
```

### Launching

To launch the native image:

```
./launch-nativeoffline.sh
```

## Docker