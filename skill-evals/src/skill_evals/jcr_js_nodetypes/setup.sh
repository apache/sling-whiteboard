#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
    echo "usage: $0 <git-revision>" >&2
    exit 2
fi

git_revision="$1"

cd /workspace/sling-org-apache-sling-jcr-js-nodetypes
git fetch --all --tags
git checkout --force "$git_revision"
