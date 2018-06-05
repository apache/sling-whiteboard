#!/bin/bash

SLING_DIR=$1

PROJECT=$2

SCRIPT_DIR=$(pwd)

function prepend () {
    echo -e "$LINE$(cat README.md)" > README.md
    #echo "PREPENDING $LINE"
}

function update_badges () {
    echo "Updating badges on $REPO"
    REPO_NAME=${PWD##*/}
    ARTIFACT_ID="$(xpath pom.xml '/project/artifactId/text()')" > /dev/null 2>&1
    echo "Artifact ID: $ARTIFACT_ID"
    
    git checkout master
    git remote remove origin
    git remote add origin git@github.com:apache/sling-$REPO_NAME.git
    git fetch
    git branch --set-upstream-to=origin/master master
    git pull
    
    echo "Adding standard items for $REPO_NAME"
    LINE="\n\n"
    prepend
    
    while IFS=, read -r ID LOC GH CONTRIB TEST TOOL DEPRECATED
    do
        if [ "$ID" == "$REPO_NAME" ]; then
            if [ "$CONTRIB" == "Y" ]; then
                LINE=" [![Contrib](http://sling.apache.org/badges/contrib.svg)](https://sling.apache.org/downloads.cgi)"
                prepend
            fi
            DEPRECATED=$(echo $DEPRECATED | xargs)
            if [ "$DEPRECATED" == "Y" ]; then
                LINE=" [![Deprecated](http://sling.apache.org/badges/deprecated.svg)](https://sling.apache.org/downloads.cgi)"
                prepend
            fi
        fi
    done < $SCRIPT_DIR/Sling-Repos.csv
    
    LINE=" [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)"
    prepend
    
    MAVEN_BADGE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://maven-badges.herokuapp.com/maven-central/org.apache.sling/$ARTIFACT_ID/badge.svg)
    if [ "$MAVEN_BADGE_RESPONSE" = "200" ]; then
        echo "Adding Maven release badge for $ARTIFACT_ID"
        LINE=" [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/$ARTIFACT_ID/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22$ARTIFACT_ID%22)"
        prepend
    else
        echo "No Maven release found for $ARTIFACT_ID"
    fi
    
    TEST_CONTENTS=$(curl -L https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)
    if [[ $TEST_CONTENTS = *"inaccessible"* || $TEST_CONTENTS = *"invalid"* ]]; then
        echo "No tests found for $REPO_NAME"
    else
        echo "Adding test badge for $REPO_NAME"
        LINE=" [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8/test_results_analyzer/)"
        prepend
    fi
    
    
    COVERAGE_CONTENTS=$(curl -L https://img.shields.io/jenkins/c/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)
    if [[ $COVERAGE_CONTENTS = *"inaccessible"* || $COVERAGE_CONTENTS = *"invalid"* ]]; then
        echo "No coverage reports found for $REPO_NAME"
    else
        echo "Adding coverage badge for $REPO_NAME"
        LINE=" [![Coverage Status](https://img.shields.io/jenkins/c/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8/)"
        prepend
    fi
    
    BUILD_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8)
    if [ "$BUILD_RESPONSE" != "404" ]; then
        echo "Adding build badge for $REPO_NAME"
        LINE=" [![Build Status](https://img.shields.io/jenkins/s/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8)"
        prepend
    else
        echo "No build found for $REPO_NAME"
    fi
    
    echo "Adding logo for $REPO_NAME"
    LINE="[<img src=\"http://sling.apache.org/res/logos/sling.png\"/>](http://sling.apache.org)\n\n"
    prepend
    
    grip -b > /dev/null 2>&1 & > /dev/null
    PID=$!
    
    echo "Commit results? (C=Commit,N=No,R=Revert)?"
    read RESULTS
    
    if [ "$RESULTS" == "C" ]; then
        git commit -a -m "Updating badges for ${REPO_NAME}"
    elif [ "$RESULTS" == "R" ]; then
        git reset --hard HEAD
    fi
    kill $PID 2>&1 > /dev/null
}

function handle_repo () {
    cd $REPO
    if [ ! -e "README.md" ]; then
        echo "No README.md found in $REPO"
    elif grep -q "http:\/\/sling\.apache\.org\/res\/logos\/sling\.png" "README.md"; then
        echo "Badge already present on $REPO, overwrite (Y/N)?"
        read OVERWRITE
        if [ "$OVERWRITE" == "Y" ]; then
            sed -i -e 1,4d README.md
            update_badges
        else
            echo "Skipping..."
        fi
    else
        update_badges
    fi
}

printf "\nStarting badge update!\n\n-------------------------\n\n"
if [ -z "$SLING_DIR" ]; then
    echo "Please provide the Sling Directory: ./add-badges.sh [SLING_DIR]"
    exit 1
fi

if [ -z "$PROJECT" ]; then
    echo "Handling all repos in $SLING_DIR"
    for REPO in $SLING_DIR/*/ ; do
        handle_repo
    done
else
    echo "Handling project $SLING_DIR/$PROJECT"
    REPO=$SLING_DIR/$PROJECT
    handle_repo
fi
printf "\n\nBadge Update Complete!"