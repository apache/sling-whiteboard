#!/bin/bash

echo "Installing dependencies..."
yum install -y wget openssl git jq

echo "Installing Apache Maven..."
mkdir mvn
curl ftp://ftp.osuosl.org/pub/apache/maven/maven-3/3.6.1/binaries/apache-maven-3.6.1-bin.tar.gz --output mvn.tar.gz
tar xzvf mvn.tar.gz --strip-components=1 -C mvn

echo "Downloading check script..."
curl 'https://gitbox.apache.org/repos/asf?p=sling-tooling-release.git;a=blob_plain;f=check_staged_release.sh;hb=HEAD' --output check_staged_release.sh

echo "Initialization completed successfully!"