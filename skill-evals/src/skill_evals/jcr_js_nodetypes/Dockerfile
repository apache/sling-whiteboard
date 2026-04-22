FROM maven:3.9.9-eclipse-temurin-8

RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

RUN git clone https://github.com/apache/sling-org-apache-sling-jcr-js-nodetypes /workspace/sling-org-apache-sling-jcr-js-nodetypes

RUN printf '%s\n' '#!/bin/sh' 'exec mvn "$@"' > /usr/local/bin/mvn8 \
    && chmod +x /usr/local/bin/mvn8
