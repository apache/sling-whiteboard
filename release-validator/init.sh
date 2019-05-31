#!/bin/bash

echo "Installing dependencies..."
yum install epel-release -y
yum install -y wget openssl git jq which

echo "Installing Java..."
yum localinstall -y $JDK_RPM

echo "Configuring JAVA_HOME..."
JAVA_VERSION=$(ls /usr/java/ | grep jdk)
echo export JAVA_HOME="/usr/java/$JAVA_VERSION" >/etc/profile.d/javaenv.sh
echo 'export PATH=$PATH:$JAVA_HOME' >> /etc/profile.d/javaenv.sh
chmod 0755 /etc/profile.d/javaenv.sh

echo "Installing Apache Maven..."
mkdir mvn
curl ftp://ftp.osuosl.org/pub/apache/maven/maven-3/3.6.1/binaries/apache-maven-3.6.1-bin.tar.gz --output mvn.tar.gz
tar xzvf mvn.tar.gz --strip-components=1 -C mvn

echo "Downloading check script..."
curl 'https://gitbox.apache.org/repos/asf?p=sling-tooling-release.git;a=blob_plain;f=check_staged_release.sh;hb=HEAD' --output check_staged_release.sh

echo "Initialization completed successfully!"