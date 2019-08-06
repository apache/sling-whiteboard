#!/bin/bash

echo "Installing dependencies..."
yum install epel-release -y
yum install -y wget openssl git jq which

echo "Installing Java..."
yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel

echo "Installing Apache Maven..."
mkdir mvn
curl ftp://ftp.osuosl.org/pub/apache/maven/maven-3/3.6.1/binaries/apache-maven-3.6.1-bin.tar.gz --output /tmp/mvn.tar.gz
tar xzvf /tmp/mvn.tar.gz --strip-components=1 -C mvn
ln -s /

echo "Configuring Java Environment Variables..."
echo export JAVA_HOME="/etc/alternatives/java_sdk_openjdk" >/etc/profile.d/javaenv.sh
echo 'export M2_HOME=/opt/mvn' >> /etc/profile.d/javaenv.sh
echo 'export MAVEN_HOME=/opt/mvn' >> /etc/profile.d/javaenv.sh
echo "export PATH=$PATH:$JAVA_HOME:${M2_HOME}/bin" >> /etc/profile.d/javaenv.sh

chmod 0755 /etc/profile.d/javaenv.sh

echo "Downloading check script..."
curl 'https://gitbox.apache.org/repos/asf?p=sling-tooling-release.git;a=blob_plain;f=check_staged_release.sh;hb=HEAD' --output check_staged_release.sh

echo "Initialization completed successfully!"