# Apache Sling Markdown Resource Provider

This module is part of the [Apache Sling](https://sling.apache.org) project.

It contains a work-in-progress markdown resource provider. The code is only lightly tested and meant as a proof-of-content
for now. The only thing worse that the code is the documentation.

## TODO

- third-level recursive JSON access fails, e.g. `curl  http://localhost:8080/md-test.3.json`
- add arbitrary properties using [YAML Front Matter](https://github.com/vsch/flexmark-java/wiki/Extensions#yaml-front-matter)
- testing
- documentation
- fix rat checks  