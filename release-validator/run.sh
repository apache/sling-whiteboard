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
    echo "Exit code: $EXIT_CODE"
    prints "Failed to execute command: $@" "error"
    exit 1
  fi
}

# Set variables
export CHECKS=${2:-000-check-signatures,001-check-ci-status}
export MVN_EXEC=/opt/mvn/bin/mvn
/etc/profile.d/javaenv.sh
export RELEASE_ID=$1
export RELEASE_FOLDER=/opt/release

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

mkdir ${RELEASE_FOLDER} 2>/dev/null

# Download the release artifacts
prints "Downloading release artifacts" "info"
try wget -e "robots=off" --wait 1 -nv -r -np "--reject=html,index.html.tmp" \
  "--follow-tags=" -P "$RELEASE_FOLDER" -nH "--cut-dirs=3" \
  "https://repository.apache.org/content/repositories/orgapachesling-${RELEASE_ID}/org/apache/sling/"

# Link the selected checks into the enabled folder
for CHECK in $(echo $CHECKS | tr "," "\n")
do
  echo "Enabling check $CHECK"
  try ln -s /opt/checks-available/$CHECK /opt/checks-enabled/$CHECK
done

# Execute the checks in checks-enabled
for CHECK in /opt/checks-enabled/*
do
  prints "Executing $CHECK" "info"
  try $CHECK
  prints "Check $CHECK executed successfully!" "success"
done

prints "All checks successful!" "success"
