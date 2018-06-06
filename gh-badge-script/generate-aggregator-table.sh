#!/bin/bash

SLING_DIR=$1

SCRIPT_DIR=$(pwd)

function write_data () {
    echo -n -e $LINE >> $SLING_DIR/aggregator/docs/modules.md
    
    if [ ! -z $FEATURE ]; then
        echo "Adding to feature $FEATURE"
        if [ -e $SLING_DIR/aggregator/docs/features/$FEATURE.md ]; then
            echo "Appending item"
            echo -n -e $LINE >> $SLING_DIR/aggregator/docs/features/$FEATURE.md
        else
            echo "Creating feature file"
            echo -n -e "[Apache Sling](http://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > [Modules](https://github.com/apache/sling-aggregator/docs/modules.md) > $FEATURE\n# $FEATURE Modules\n\n| Module | Description | Module&nbsp;Status |\n|---	|---	|---    |" > $SLING_DIR/aggregator/docs/features/$FEATURE.md
            echo -n -e $LINE >> $SLING_DIR/aggregator/docs/features/$FEATURE.md
        fi
    fi
}

function add_repo () {
    echo "Fetching badges on $REPO"
    
    cd $REPO
    REPO_NAME=${PWD##*/}
    
    ARTIFACT_ID="$(xpath pom.xml '/project/artifactId/text()')" > /dev/null 2>&1
    echo "Artifact ID: $ARTIFACT_ID"
    
    if [[ ! -z $ARTIFACT_ID ]]; then
    
        FEATURE=""
        while IFS=, read -r ID LOC GH CONTRIB TEST TOOL DEPRECATED FEATURE BASH
        do
            if [ "$ID" == "$REPO_NAME" ]; then
                FEATURE=$(echo $FEATURE | xargs | tr -dc '[:alnum:]')
                break
            fi
        done < $SCRIPT_DIR/Sling-Repos.csv
        echo "Feature: $FEATURE"
        
        NAME="$(xpath pom.xml '/project/name/text()' | xargs)" > /dev/null 2>&1
        DESCRIPTION="$(xpath pom.xml '/project/description/text()' | xargs)" > /dev/null 2>&1
        
        echo "Adding standard items for $REPO_NAME"
        LINE="\n| [$NAME](https://github.com/apache/sling-$REPO_NAME) <br/> <small>([$ARTIFACT_ID](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22$ARTIFACT_ID%22))</small> | $DESCRIPTION | "
        write_data
        
        BUILD_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8)
        if [ "$BUILD_RESPONSE" != "404" ]; then
            echo "Adding build badge for $REPO_NAME"
            LINE="&#32;[![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-$REPO_NAME-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8)"
            write_data
        else
            echo "No build found for $REPO_NAME"
        fi

        TEST_CONTENTS=$(curl -L https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)
        if [[ $TEST_CONTENTS = *"inaccessible"* || $TEST_CONTENTS = *"invalid"* ]]; then
            echo "No tests found for $REPO_NAME"
        else
            echo "Adding test badge for $REPO_NAME"
            LINE="&#32;[![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8/test_results_analyzer/)"
            write_data
        fi
        
        COVERAGE_CONTENTS=$(curl -L https://img.shields.io/jenkins/c/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)
        if [[ $COVERAGE_CONTENTS = *"inaccessible"* || $COVERAGE_CONTENTS = *"invalid"* ]]; then
            echo "No coverage reports found for $REPO_NAME"
        else
            echo "Adding coverage badge for $REPO_NAME"
            LINE="&#32;[![Coverage Status](https://img.shields.io/jenkins/c/https/builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8.svg)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-$REPO_NAME-1.8/)"
            write_data
        fi
        
        if [[ ! -z $ARTIFACT_ID ]]; then
            JAVADOC_BADGE_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://www.javadoc.io/badge/org.apache.sling/$ARTIFACT_ID.svg)
            if [ $JAVADOC_BADGE_RESPONSE != "404" ]; then
                echo "Adding Javadoc badge for $ARTIFACT_ID"
                LINE="&#32;[![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/$ARTIFACT_ID.svg)](https://www.javadoc.io/doc/org.apache.sling/$ARTIFACT_ID)"
                write_data
            else
                echo "No published javadocs found for $ARTIFACT_ID"
            fi

            MAVEN_BADGE_CONTENTS=$(curl -L https://maven-badges.herokuapp.com/maven-central/org.apache.sling/$ARTIFACT_ID/badge.svg)
            if [[ $MAVEN_BADGE_CONTENTS = *"unknown"* ]]; then
                echo "No Maven release found for $ARTIFACT_ID"
            else
                echo "Adding Maven release badge for $ARTIFACT_ID"
                LINE="&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/$ARTIFACT_ID/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22$ARTIFACT_ID%22)"
                write_data
            fi
        fi

        while IFS=, read -r ID LOC GH CONTRIB TEST TOOL DEPRECATED FEATURE BASH
        do
            if [ "$ID" == "$REPO_NAME" ]; then
                if [ "$CONTRIB" == "Y" ]; then
                    LINE="&#32;[![Contrib](http://sling.apache.org/badges/contrib.svg)](https://sling.apache.org/downloads.cgi)"
                    write_data
                fi
                DEPRECATED=$(echo $DEPRECATED | xargs)
                if [ "$DEPRECATED" == "Y" ]; then
                    LINE="&#32;[![Deprecated](http://sling.apache.org/badges/deprecated.svg)](https://sling.apache.org/downloads.cgi)"
                    write_data
                fi
                FEATURE=$(echo $FEATURE | xargs)
                if [ ! -z "$FEATURE" ]; then
                    LINE="&#32;[![${FEATURE}](https://sling.apache.org/badges/feature-$FEATURE.svg)](https://github.com/apache/sling-aggregator/docs/features/$FEATURE.md)"
                    write_data
                fi
            fi
        done < $SCRIPT_DIR/Sling-Repos.csv
    
        LINE=" |"
        write_data
    fi
}

if [ ! -f ~/.grip/settings.py ]; then
    echo "Did not find GitHub Access token file, please generate an access token on GitHub https://github.com/settings/tokens/new?scopes= and provide it below:"
    read ACCESS_TOKEN
    echo "PASSWORD = '$ACCESS_TOKEN'" > ~/.grip/settings.py
fi

printf "\nAggregator Table Generation!\n\n-------------------------\n\n"
if [ -z "$SLING_DIR" ]; then
    echo "Please provide the Sling Directory: ./generate-aggregator-table.sh [SLING_DIR]"
    exit 1
fi

rm -rf $SLING_DIR/aggregator/docs/features/* > /dev/null
mkdir $SLING_DIR/aggregator/docs
mkdir $SLING_DIR/aggregator/docs/features
echo -e -n "[Apache Sling](http://sling.apache.org) > [Aggregator](https://github.com/apache/sling-aggregator/) > Modules\n# Modules\n\n| Module | Description | Module&nbsp;Status |\n|---	|---	|---    |" > $SLING_DIR/aggregator/docs/modules.md

echo "Handling all repos in $SLING_DIR"
for REPO in $SLING_DIR/*/ ; do
    add_repo
done
echo "Table Generation Complete!"