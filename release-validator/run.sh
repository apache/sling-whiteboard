#!/bin/bash

prints() {
  if [ "$2" == "info" ]; then
    COLOR="96m";
  elif [ "$2" == "success" ]; then
    COLOR="92m";
  elif [ "$2" == "error" ]; then
    COLOR="91m";
  else 
    COLOR="0m";
  fi
  STARTCOLOR="\e[$COLOR";
  ENDCOLOR="\e[0m";
  printf "\n\n$STARTCOLOR%b$ENDCOLOR" "$1\n";
}

mkdir tmp

prints "Starting Validation for Apache Sling Release #$RELEASE_ID" "info"

prints "Loading PGP Keys..." "info"
curl https://people.apache.org/keys/group/sling.asc --output sling.asc || exit 1
gpg --import sling.asc || exit 1

prints "Validating release signatures..." "info"
CHECK_RESULT=$(/bin/bash check_staged_release.sh $RELEASE_ID tmp)
printf "\n$CHECK_RESULT\n"
if [[ "$CHECK_RESULT" == *"BAD"* ]]; then
  prints "Loading PGP Keys..." "error"
  echo "Check(s) Failed!"
  exit 1
elif [[ "$CHECK_RESULT" = *"no files found"* ]]; then
  prints "Staging repository ${RELEASE_ID} not found!" "error"
  exit 1
else
  prints "Release signatures check successful!" "success"
fi

mkdir ~/.m2
cat > ~/.m2/settings.xml <<EOF
<settings>
 <profiles>
   <profile>
     <id>staging</id>
     <repositories>
       <repository>
         <id>staging-repo</id>
         <name>your custom repo</name>
         <url>https://repository.apache.org/content/repositories/orgapachesling-$RELEASE_ID</url>
       </repository>
     </repositories>
   </profile>
 </profiles>
 <activeProfiles>
   <activeProfile>staging</activeProfile>
  </activeProfiles>
</settings>
EOF

HAS_BUNDLE=false
for RELEASE_FOLDER in tmp/${RELEASE_ID}/org/apache/sling/*
do
  if [[ -f $RELEASE_FOLDER ]]; then
    continue
  fi
  prints "Running build for $RELEASE_FOLDER" "info"
  
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
  printf "Resolved Variables:\n\tArtifact ID: $MVN_ARTIFACT_ID\n\tPackaging: $MVN_PACKAGING\n\tRepo Slug: $REPO\n\tVersion: $MVN_VERSION\n"

  prints "Checking out code from https://github.com/apache/$REPO.git..." "info"
  git clone https://github.com/apache/$REPO.git || exit 1
  cd $REPO
  git checkout $MVN_ARTIFACT_ID-$MVN_VERSION || exit 1

  prints "Building $MVN_ARTIFACT_ID..." "info"
  ../mvn/bin/mvn clean install || exit 1
  
  if [[ "$MVN_PACKAGING" == "bundle" ]]; then
  	HAS_BUNDLE=true
  fi

  cd ..  
done

prints "Build(s) Successful!" "success"

if [ "$HAS_BUNDLE" = true ]; then
  prints "Bundles found, starting Apache Sling Starter..." "info"
  
  mkdir run
  
  echo "Downloading Sling Starter..."
  mvn/bin/mvn -q dependency:get -DremoteRepositories=https://repository.apache.org/content/groups/snapshots -DgroupId=org.apache.sling -DartifactId=org.apache.sling.starter -Dversion=LATEST -Dtransitive=false
  mvn/bin/mvn -q dependency:copy -Dartifact=org.apache.sling:org.apache.sling.starter:LATEST -DoutputDirectory=run
  
  echo "Starting Sling Starter..."

  mkdir -p run/sling/logs
  (
    (
      java -server -Xmx1024m -XX:MaxPermSize=256M -Djava.awt.headless=true -jar run/*.jar  -p 8080 -c run/sling &
    echo $! > app.pid
    ) >> run/sling/logs/stdout.log 2>&1
  ) &
  
  echo "Waiting for Sling to fully start..."

  STARTED=false
  ATTEMPT=0
  while [ $ATTEMPT -lt 5 ]; do
    sleep 30
    RESP=$(curl -s http://localhost:8080/starter/index.html)
    if [[ "$RESP" == *"Do not remove this comment, used for Starter integration tests"* ]]; then
      prints "Sling Starter started!" "success"
      let STARTED=true
      break
    else
      echo "Not yet started..."
    fi
    let ATTEMPT=ATTEMPT+1 
  done
  
  if [[ $STARTED = false ]]; then
    prints "Sling failed to start!" "error"
    exit 1
  fi
  
  BUNDLE_SUCCESS=true
  prints "Installing bundles..." "info"
  for RELEASE_FOLDER in tmp/${RELEASE_ID}/org/apache/sling/*
  do
    if [[ -f $RELEASE_FOLDER ]]; then
      continue
    fi
    
    MVN_PACKAGING=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.packaging}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
    if [[ "$MVN_PACKAGING" = "bundle" ]]; then
    
      MVN_VERSION=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.version}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
      MVN_ARTIFACT_ID=$(mvn/bin/mvn -q -Dexec.executable=echo  -Dexec.args='${project.artifactId}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
      REPO="sling-${MVN_ARTIFACT_ID//\./-}"
      
      echo "Installing / starting bundle $REPO/target/$MVN_ARTIFACT_ID-$MVN_VERSION.jar..."
      curl -u admin:admin -F action=install -F bundlestart=true -F refreshPackages=true -F bundlestartlevel=20 -F bundlefile=@"$REPO/target/$MVN_ARTIFACT_ID-$MVN_VERSION.jar" http://127.0.0.1:8080/system/console/bundles || exit 1
      
      STATE=""
      ATTEMPT=0
      while [ $ATTEMPT -lt 12 ]; do
        sleep 10
        BUNDLE_RESPONSE=$(curl -s -u admin:admin http://127.0.0.1:8080/system/console/bundles/$MVN_ARTIFACT_ID.json)
        STATE=$(echo $BUNDLE_RESPONSE | jq -r '.data[0].state')
        IMPORTS_IN_ERROR=$(echo $BUNDLE_RESPONSE | jq -r '.data[0].props[] | select(.key == "Imported Packages").value[] | select( contains("ERROR") )')
        if [[ "$STATE" == "Active" ]]; then
          prints "Bundle $MVN_ARTIFACT_ID started successfully!" "success"
          break
        else
          echo "Bundle is currently in state $STATE, waiting to see if it starts..."
        fi
        let ATTEMPT=ATTEMPT+1
      done
      
      if [[ "$STATE" != "Active" ]]; then
        prints "Failed to start $MVN_ARTIFACT_ID, current state: $STATE" "error"
        printf "Imports in error state:\n$IMPORTS_IN_ERROR\n\n"
        BUNDLE_SUCCESS=false
      fi
    else
      echo "Ignoring non-bundle ${RELEASE_FOLDER}..."
    fi
  done
  
  if [[ $BUNDLE_SUCCESS == true ]]; then
    prints "Release ${RELEASE_ID} verified successfully!" "success"
  else 
    prints "Some bundles failed to start" "error"
  fi
  
  if [[ "$KEEP_RUNNING" == "true" ]]; then
    TIMEOUT="${RUN_TIMEOUT:=10m}"
  	echo "Leaving Sling Starter running for $TIMEOUT for testing..."
    
    CONTAINER_ID=$(cat /etc/hostname)
  	
  	echo "Run the following command to see the URL to connect to the Sling Starter:"
  	printf "\tdocker port $CONTAINER_ID 8080\n"
  	echo "If you are satisfied, the container can be stopped with:"
  	printf "\tdocker stop $CONTAINER_ID\n"

    sleep $TIMEOUT
  fi
else
  echo "Packaging is $MVN_PACKAGING, not bundle"
  prints "Release $RELEASE_ID verified successfully!" "success"
fi