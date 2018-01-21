#!/usr/bin/env bash

#    Licensed to the Apache Software Foundation (ASF) under one
#    or more contributor license agreements.  See the NOTICE file
#    distributed with this work for additional information
#    regarding copyright ownership.  The ASF licenses this file
#    to you under the Apache License, Version 2.0 (the
#    "License"); you may not use this file except in compliance
#    with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing,
#    software distributed under the License is distributed on an
#    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#    KIND, either express or implied.  See the License for the
#    specific language governing permissions and limitations
#    under the License.

dir=`pwd`
echo "Build the Sling Project Archetype in folder: $dir"
mvn clean install

testFolder=$dir/target/test-classes/projects/allMerged/project/sample-test-all

echo
echo
echo "--------------------------------------------------"
echo "   Build and Deploy the All (Merged) Test Project"
echo "--------------------------------------------------"
echo
echo

cd $testFolder
mvn clean install -P autoInstallAll

testFolder=$dir/target/test-classes/projects/notAllMerged/project/sample-test-ui

echo
echo
echo "---------------------------------------------------"
echo "Build and Deploy the Not All (Merged) Test Project"
echo "---------------------------------------------------"
echo
echo

cd $testFolder
mvn clean install -P autoInstallPackage

echo
echo
echo "------------------------------------------"
echo "         Done"
echo "------------------------------------------"
echo
echo

cd $dir
