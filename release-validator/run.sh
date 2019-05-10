#!/bin/bash

mkdir tmp

echo "Loading PGP Keys..."
curl https://people.apache.org/keys/group/sling.asc --output sling.asc || exit 1
gpg --import sling.asc || exit 1

echo "Validating release..."
CHECK_RESULT=$(/bin/bash check_staged_release.sh ${RELEASE_ID} tmp)
printf "\n$CHECK_RESULT\n"
if [[ "$CHECK_RESULT" == *"BAD"* ]]; then
  echo "Check(s) Failed!"
  exit 1
elif [[ "$CHECK_RESULT" = *"no files found"* ]]; then
  echo "Staging repository ${RELEASE_ID} not found!"
  exit 1
else
  echo "Check successful!"
fi

HAS_BUNDLE=false
for RELEASE_FOLDER in tmp/${RELEASE_ID}/org/apache/sling/*
do
  echo "Running build for $RELEASE_FOLDER"
  
  echo "Resolving Maven Variables..."
  MVN_PACKAGING=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.packaging}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
  MVN_VERSION=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.version}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
  MVN_ARTIFACT_ID=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.artifactId}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
  if [[ $MVN_ARTIFACT_ID == "sling-"* ]]; then
    echo "Artifact ID starts with sling-, assuming it will not be duplicated..."
    REPO="${MVN_ARTIFACT_ID//\./-}"
  else
    REPO="sling-${MVN_ARTIFACT_ID//\./-}"
  fi

  echo "Checking out code from https://github.com/apache/$REPO.git..."
  git clone https://github.com/apache/$REPO.git || exit 1
  cd $REPO
  git checkout $MVN_ARTIFACT_ID-$MVN_VERSION || exit 1

  echo "Building project..."
  ../mvn/bin/mvn clean install || exit 1
  
  if [ "$MVN_PACKAGING" == "bundle" ]
  then
  	HAS_BUNDLE=true
  fi

  cd ..  
done


if [ "$HAS_BUNDLE" = true ];
then
  echo "Installing bundle..."
  
  mkdir run
  
  echo "Downloading Sling Starter..."
  mvn/bin/mvn -q dependency:get -DremoteRepositories=https://repository.apache.org/content/groups/snapshots -DgroupId=org.apache.sling -DartifactId=org.apache.sling.starter -Dversion=LATEST -Dtransitive=false
  mvn/bin/mvn -q dependency:copy -Dartifact=org.apache.sling:org.apache.sling.starter:LATEST -DoutputDirectory=run
  
  echo "Starting Sling Starter..."

  mkdir -p sling/logs
  (
    (
      java -server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true -jar run/*.jar  -p 8080 &
    echo $! > app.pid
    ) >> sling/logs/stdout.log 2>&1
  ) &
  
  echo "Waiting for Sling to fully start..."

  while true; do
    sleep 30
    RESP=$(curl -s http://localhost:8080/starter/index.html)
    if [[ "$RESP" == *"Do not remove this comment, used for Starter integration tests"* ]]; then
      echo "Sling Starter started!"
    else
      echo "Not yet started..."
      break
    fi
  done
  
  echo "Installing bundles..."
  for RELEASE_FOLDER in tmp/${RELEASE_ID}/org/apache/sling/*
  do
  
    MVN_PACKAGING=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.packaging}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
    if [[ "$MVN_PACKAGING" = "bundle" ]] ; then
      echo "Installing bundle ${RELEASE_FOLDER}..."
    
      MVN_VERSION=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.version}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
      MVN_ARTIFACT_ID=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.artifactId}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
      REPO="sling-${MVN_ARTIFACT_ID//\./-}"
  
      curl -u admin:admin -F action=install -F bundlestartlevel=20 -F bundlefile=@"$REPO/target/$MVN_ARTIFACT_ID-$MVN_VERSION.jar" http://127.0.0.1:8080/system/console/bundles || exit 1
    else
      echo "Ignoring non-bundle ${RELEASE_FOLDER}..."
    fi
  done
  
  echo "Release ${RELEASE_ID} verified successfully!"
  
  if [[ "$KEEP_RUNNING" == "true" ]]; then
  	echo "Leaving Sling Starter running for 10 minutes for testing..."
  	
  	printf "Run the following command to see the URL to connect to the Sling Starter under the PORT parameter:\n"
  	printf "\tdocker ps | grep sling-check-release"

    sleep 10m
  fi
else

  echo "Packaging is $MVN_PACKAGING, not bundle"
  
  echo "Release ${RELEASE_ID} verified successfully!"
fi