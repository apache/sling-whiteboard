<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
# Feature Launcher IT Example (SLING-13233)

A minimal, **fully build-local** integration-test setup: a content package that embeds a bundle is
converted to a feature model, aggregated with the Sling Starter, and launched — with all converted
artifacts resolved from the build's own `target/` directory, so nothing is installed into `~/.m2`.

## Modules

| Module | Packaging | Purpose |
|--------|-----------|---------|
| `bundle` | `bundle` | A trivial OSGi DS component (proof that "something" reaches `ACTIVE`). |
| `content` | `content-package` | Some dummy JCR content under `/content/fmexample`. |
| `all` | `content-package` | Container package embedding the bundle and the content package. |
| `it.tests` | `jar` | Converts → aggregates → launches → tests. |

## The pipeline (all in `it.tests`)

1. **`sling-feature-converter-maven-plugin:convert-cp`** (`initialize`) converts the `all` package into a
   feature model under `target/cp-conversion/fm.out/` and writes the converted bundles + residual
   `*-cp2fm-converted.zip` content packages under `target/cp-conversion/`. **`installConvertedCP=false`** keeps
   them build-local (nothing is copied to `~/.m2`).
2. **`slingfeature-maven-plugin:aggregate-features`** (`generate-resources`) aggregates the Sling Starter
   (`<includeArtifact>`) with the converted feature, read at goal time via **`<additionalFeatureFiles>`**.
   **`<osgiBsnCollisionDetection>`** enforces OSGi `Bundle-SymbolicName` uniqueness (collisions resolved by
   the `*:*:HIGHEST` override).
3. **`feature-launcher-maven-plugin`** launches the aggregated feature. **`<additionalRepositoryUrls>`**
   points at `target/cp-conversion`, which is consulted *before* the local/remote repositories, so the
   build-local converted artifacts resolve without remote lookups (and without an `~/.m2` install).
4. **`maven-failsafe-plugin`** runs `ExampleBundleIT`, which asserts the example bundle is `ACTIVE` on the
   launched instance.

## Plugin features demonstrated (SLING-13233)

- `slingfeature-maven-plugin`: `<additionalFeatureFiles>` and `<osgiBsnCollisionDetection>`.
- `feature-launcher-maven-plugin`: `<additionalRepositoryUrls>` (extends, rather than replaces, the default
  repository list).
- `sling-feature-converter-maven-plugin`: `installConvertedCP=false` to stay build-local.

## Run it

```
mvn clean verify
```
