/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.*;
import java.util.*;
    
def archive = new File(basedir, "target/content-package-1.0.0-SNAPSHOT.zip" );

assert archive.exists() && archive.isFile() : "Could not find generated archive: " + archive;

def found = false;
def zipFile = new java.util.zip.ZipFile(archive);

def entryNames = new ArrayList<String>();
def entries = zipFile.entries();
while(entries.hasNext()){
    def name = entries.next().getName();
    entryNames.add(name);
    if(name.equals("jcr_root/apps/test/config/org.apache.sling.jcr.repoinit.RepositoryInitializer~simple.cfg.json")){
        found = true;
    }
}
assert found : "Did not find expected entry archive: " + entryNames;