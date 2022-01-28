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

Apache Sling RepoInit Maven Plugin Usage
======================================

# Basic Example

Parse, verify and convert repoinit text files into JSON OSGi configuration files and then build a content package containing the configurations.

Note that you must include the executions for the plugin to work. 

    <build>
        <sourceDirectory>src/main/content/jcr_root</sourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.apache.sling</groupId>
                <artifactId>org.apache.sling.maven.repoinit</artifactId>
                <version>${currentStableVersion}</version>
                <executions>
                    <!-- Parses the RepoInit scripts -->
                    <execution>
                        <id>parse</id>
                        <goals>
                            <goal>parse</goal>
                        </goals>
                    </execution>
                    <!-- Verify the scripts by loading them in a Mock JCR instance -->
                    <execution>
                        <id>verify</id>
                        <goals>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                    <!-- Convert the RepoInit scripts into JSON OSGi Configurations -->
                    <execution>
                        <id>convert</id>
                        <goals>
                            <goal>to-osgi-config</goal>
                        </goals>
                        <configuration>
                            <!-- 
                                This must be in the jcr_root directory and filter.

                                The filevault-maven-plugin creates the package from the src directory by default.
                                
                                If you wanted this file to not be in source control, you could add the pattern:
                                src/main/jcr_root/content/apps/test/config/org.apache.sling.jcr.repoinit.RepositoryInitializer.* 
                                to your .gitignore
                             -->
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
                            <!-- Note: this contains the conversion outputDir -->
                            <root>/apps/test</root>
                        </filter>
                    </filters>
                    <group>com.test</group>
                    <name>test.all</name>
                    <allowIndexDefinitions>true</allowIndexDefinitions>
                    <noIntermediateSaves>false</noIntermediateSaves>
                    <packageType>container</packageType>
                    <jcrRootSourceDirectory>src/main/content/jcr_root</jcrRootSourceDirectory>
                </configuration>
            </plugin>
        </plugins>
    </build>


## Configuring Dependency Versions

To specify a different version of one of the dependencies, set the dependency within the plugin as such:

    <build>
        <sourceDirectory>src/main/content/jcr_root</sourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.apache.sling</groupId>
                <artifactId>org.apache.sling.maven.repoinit</artifactId>
                <version>${currentStableVersion}</version>
                <executions>
                    <!-- Execution Config Here -->
                </executions>
                <!-- Use older dependency versions -->
                <dependencies>
                    <dependency>
                        <groupId>org.apache.sling</groupId>
                        <artifactId>org.apache.sling.repoinit.parser</artifactId>
                        <version>1.3.2</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.sling</groupId>
                        <artifactId>org.apache.sling.jcr.repoinit</artifactId>
                        <version>1.1.16</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.sling</groupId>
                        <artifactId>org.apache.sling.testing.sling-mock-oak</artifactId>
                        <version>3.0.0-1.16.0</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
