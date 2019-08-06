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

try() { 
  "$@"
  local EXIT_CODE=$?

  if [[ $EXIT_CODE -ne 0 ]]; then
    prints "Failed to execute command: $@" "error"
    exit 1
  fi
}

# Downloads the Sling PGP Keys and validates the release artifacts
validate_signatures () {
  prints "Validating signatures..." "info"

  mkdir release
  
  echo "Loading PGP Keys..."
  try curl https://people.apache.org/keys/group/sling.asc --output sling.asc
  try gpg --import sling.asc

  prints "Validating release signatures..." "info"
  CHECK_RESULT=$(/bin/bash check_staged_release.sh $RELEASE_ID release)
  printf "\n$CHECK_RESULT\n"
  if [[ "$CHECK_RESULT" == *"BAD"* ]]; then
    prints "Check(s) Failed!" "error"
    exit 1
  elif [[ "$CHECK_RESULT" = *"no files found"* ]]; then
    prints "Staging repository ${RELEASE_ID} not found!" "error"
    exit 1
  else
    prints "Release signatures validated successful!" "success"
  fi
}

# Build the release artifacts using Apache Maven
build_releases () {
  for RELEASE_FOLDER in release/${RELEASE_ID}/org/apache/sling/*
  do
    if [[ -f $RELEASE_FOLDER ]]; then
      continue
    fi
    prints "Running build for $RELEASE_FOLDER" "info"

    echo "Resolving Maven Variables..."
    MVN_PACKAGING=$($MVN_EXEC -q -Dexec.executable=echo  -Dexec.args='${project.packaging}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
    MVN_VERSION=$($MVN_EXEC -q -Dexec.executable=echo  -Dexec.args='${project.version}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
    MVN_ARTIFACT_ID=$($MVN_EXEC -q -Dexec.executable=echo  -Dexec.args='${project.artifactId}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
    REPO=$($MVN_EXEC -q -Dexec.executable=echo  -Dexec.args='${project.scm.developerConnection}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
    REPO=${REPO/scm:git:/}

    if [[ $REPO != *.git ]]; then
      prints "Skipping sub-module ${MVN_ARTIFACT_ID}..." "info"
      continue
    fi
    printf "Resolved Variables:\n\tArtifact ID: $MVN_ARTIFACT_ID\n\tPackaging: $MVN_PACKAGING\n\tSCM Repository: $REPO\n\tVersion: $MVN_VERSION\n"

    prints "Checking out code from $REPO..." "info"
    try git clone "$REPO" "build/$MVN_ARTIFACT_ID"
    cd build/$MVN_ARTIFACT_ID
    try git checkout $MVN_ARTIFACT_ID-$MVN_VERSION

    prints "Building $MVN_ARTIFACT_ID..." "info"
    try $MVN_EXEC clean install

    if [[ "$MVN_PACKAGING" == "bundle" ]]; then
      echo "Found bundle artifact..."
      HAS_BUNDLE=true
    fi

    cd /opt
  done
  prints "Build(s) Successful!" "success"
}

# Starts up Apache Sling using the Integration Testing project
start_sling () {
  echo "Downloading latest Sling Starter..."
  try $MVN_EXEC -q -U dependency:copy -Dartifact=org.apache.sling:org.apache.sling.starter:LATEST -DoutputDirectory=run
  
  prints "Starting Sling Starter..." "info"
  mkdir -p run/sling/logs
  (
    (
      java -jar run/org.apache.sling.starter-*.jar -c run/sling &
      echo $! > run/app.pid
    ) >> run/sling/logs/stdout.log 2>&1
  ) &
  
  echo "Waiting for Sling to fully start..."

  STARTED=false
  ATTEMPT=0
  while [ $ATTEMPT -lt 10 ]; do
    sleep 60
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
}

install_bundles () {
  BUNDLE_SUCCESS=true
  prints "Installing bundles..." "info"
  for RELEASE_FOLDER in release/${RELEASE_ID}/org/apache/sling/*
  do
    if [[ -f $RELEASE_FOLDER ]]; then
      continue
    fi
    echo "Checking release folder ${RELEASE_FOLDER}..."
    
    MVN_PACKAGING=$($MVN_EXEC -q -Dexec.executable=echo  -Dexec.args='${project.packaging}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
    if [[ "$MVN_PACKAGING" = "bundle" ]]; then
    
      MVN_VERSION=$($MVN_EXEC -q -Dexec.executable=echo  -Dexec.args='${project.version}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
      MVN_ARTIFACT_ID=$($MVN_EXEC -q -Dexec.executable=echo  -Dexec.args='${project.artifactId}' --non-recursive  exec:exec -f $RELEASE_FOLDER/**/*.pom)
      REPO="sling-${MVN_ARTIFACT_ID//\./-}"
      ARTIFACT="/opt/build/${MVN_ARTIFACT_ID}/target/${MVN_ARTIFACT_ID}-${MVN_VERSION}.jar"
      
      if [ ! -f "${ARTIFACT}" ]; then
        prints "Skipping ${RELEASE_FOLDER} not find expected artifact: ${ARTIFACT}" "error"
        continue
      fi
      
      prints "Installing / starting bundle ${ARTIFACT}..." "info"
      try curl -u admin:admin -F action=install -F bundlestart=true -F refreshPackages=true -F bundlestartlevel=20 -F bundlefile=@"${ARTIFACT}" http://127.0.0.1:8080/system/console/bundles
      
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
    prints "All bundes in release #${RELEASE_ID} installed successfully!" "success"
  else 
    prints "Some bundles failed to start" "error"
  fi
}

# Set variables
MVN_EXEC=/opt/mvn/bin/mvn
/etc/profile.d/javaenv.sh
RELEASE_ID=$1
HAS_BUNDLE=false

# Set the Maven repo so that we can pull the other release artifacts in a multi-artifact release
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

# Start of the release process
prints "Starting Validation for Apache Sling Release #$RELEASE_ID" "info"

validate_signatures

build_releases

if [ "$HAS_BUNDLE" = true ]; then
  prints "Bundles found, starting Apache Sling Starter..." "info"
  
  start_sling
  
  install_bundles
  
  prints "Release #$RELEASE_ID verified successfully!" "success"
  
else
  echo "No bundles found in built artifacts..."
  prints "Release #$RELEASE_ID verified successfully!" "success"
fi