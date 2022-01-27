  <!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
    -->

Apache Sling RepoInit Maven Plugin
======================================

Maven Plugin for parsing and converting Sling RepoInit scripts to OSGi Configuration format.

See [Goals](plugin-info.html) for a list of supported goals.

### Usage

Parse and then convert repoinit text files into JSON OSGi configuration files and then
builds a content package containing the configurations.

    <build>
        <sourceDirectory>src/main/content/jcr_root</sourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.apache.sling</groupId>
                <artifactId>org.apache.sling.maven.repoinit</artifactId>
                <version>${currentStableVersion}</version>
                <executions>
                    <execution>
                        <id>parse</id>
                        <goals>
                            <goal>parse</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>convert</id>
                        <goals>
                            <goal>to-osgi-config</goal>
                        </goals>
                        <configuration>
                            <outputDir>${project.basedir}/src/main/content/jcr_root/apps/test/config</outputDir>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.jackrabbit</groupId>
                <artifactId>filevault-package-maven-plugin</artifactId>
                <version>1.2.2</version>
                <extensions>true</extensions>
                <configuration>
                    <filters>
                        <filter>
                            <root>/apps/test</root>
                        </filter>
                    </filters>
                    <group>com.test</group>
                    <name>test.ui.apps</name>
                    <allowIndexDefinitions>true</allowIndexDefinitions>
                    <noIntermediateSaves>false</noIntermediateSaves>
                    <packageType>container</packageType>
                    <jcrRootSourceDirectory>src/main/content/jcr_root</jcrRootSourceDirectory>
                </configuration>
            </plugin>
        </plugins>
    </build>

Additional examples can be found in the [integration tests](https://github.com/apache/sling-repoinit-maven-plugin/blob/master/it/projects)
