FROM maven:3.9.9-eclipse-temurin-8

ENV MAVEN_ARGS="--batch-mode --no-transfer-progress"

RUN apt-get update \
    && apt-get install -y --no-install-recommends git jq \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

RUN git clone https://github.com/apache/sling-org-apache-sling-jcr-js-nodetypes /workspace/sling-org-apache-sling-jcr-js-nodetypes

COPY dataset.jsonl /tmp/jcr_js_nodetypes-dataset.jsonl

RUN cd /workspace/sling-org-apache-sling-jcr-js-nodetypes \
    && for revision in $(jq -r '.git_revision' /tmp/jcr_js_nodetypes-dataset.jsonl); do \
        git checkout --force "$revision" \
        && mvn org.apache.maven.plugins:maven-dependency-plugin:3.10.0:go-offline; \
    done \
    && git checkout --force master \
    && rm -f /tmp/jcr_js_nodetypes-dataset.jsonl

RUN printf '%s\n' '#!/bin/sh' 'exec mvn "$@"' > /usr/local/bin/mvn8 \
    && chmod +x /usr/local/bin/mvn8
