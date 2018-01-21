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

import java.util.regex.Pattern
import org.apache.commons.io.FileUtils

def rootDir = new File(request.getOutputDirectory() + "/" + request.getArtifactId())

def coreBundle = new File(rootDir, "core")
def coreSrc = new File(coreBundle, "src")
def coreExampleBundle = new File(rootDir, "core.example")
def coreExampleSrc = new File(coreExampleBundle, "src")

def uiAppsPackage = new File(rootDir, "ui.apps")
def uiAppsSrc = new File(uiAppsPackage, "src")
def uiAppsExamplePackage = new File(rootDir, "ui.apps.example")
def uiAppsExampleSrc = new File(uiAppsExamplePackage, "src")

def uiAppsPom = new File(uiAppsPackage, "pom.xml")
def allPackage = new File(rootDir, "all")
def rootPom = new File(rootDir, "pom.xml")
def readme = new File(rootDir, "README.md")
def readmeAll = new File(rootDir, "README.All.md")
def readmeNotAll = new File(rootDir, "README.NotAll.md")

def optionAll = request.getProperties().get("optionAll")
def optionExample = request.getProperties().get("optionExample")


// helper methods

// Remove the given Module from the parent POM
def removeModule(pomFile, moduleName) {
    def pattern = Pattern.compile("\\s*<module>" + Pattern.quote(moduleName) + "</module>", Pattern.MULTILINE)
    def pomContent = pomFile.getText("UTF-8")
    pomContent = pomContent.replaceAll(pattern, "")
    pomFile.newWriter().withWriter { w ->
        w << pomContent
    }
}

// Either remove the tag lines or the line plus the content in between
// forAll = true: removes all content between @startForNotAll@ and @endForNotAll@
// forAll = false: emoves all content between @startForAll@ and @endForAll@
def removeTags(pomFile, forAll) {
    if(!forAll) {
        // Remove all lines for Not All and remove all content inside for All
        def startPattern = Pattern.compile("\\s*<!-- @startForNotAll@ .*-->")
        def endPattern = Pattern.compile("\\s*<!-- @endForNotAll@ .*-->")
        def wrapPattern = Pattern.compile("\\s*<!-- @startForAll@ [\\s\\S]*?<!-- @endForAll@ .*-->")

        def pomContent = pomFile.getText("UTF-8")
        pomContent = pomContent.replaceAll(startPattern, "")
        pomContent = pomContent.replaceAll(endPattern, "")
        pomContent = pomContent.replaceAll(wrapPattern, "")
        pomFile.newWriter().withWriter { w ->
            w << pomContent
        }
    } else {
        // Remove all lines for All and remove all content inside for Not All
        def wrapPattern = Pattern.compile("\\s*<!-- @startForNotAll@ [\\s\\S]*?<!-- @endForNotAll@ .*-->")
        def startPattern = Pattern.compile("\\s*<!-- @startForAll@ .*-->")
        def endPattern = Pattern.compile("\\s*<!-- @endForAll@ .*-->")

        def pomContent = pomFile.getText("UTF-8")
        pomContent = pomContent.replaceAll(wrapPattern, "")
        pomContent = pomContent.replaceAll(startPattern, "")
        pomContent = pomContent.replaceAll(endPattern, "")
        pomFile.newWriter().withWriter { w ->
            w << pomContent
        }
    }
}

if(optionAll == "n") {
    // Remove All Package / Module
    assert allPackage.deleteDir()
    removeModule(rootPom, "all")
    // Remove content for 'All' and remove tag lines for Not All
    removeTags(uiAppsPom, false)
    // Delete the Readme.md for All
    assert readmeAll.delete()
    // Rename the Not For All Readme to the Readme.md file
    assert readmeNotAll.renameTo(readme)
} else {
    // Remove content for 'Not All' and remove tag lines for All
    removeTags(uiAppsPom, true)
    // Delete the Readme.md for Not All
    assert readmeNotAll.delete()
    // Rename the For All Readme to the Readme.md file
    assert readmeAll.renameTo(readme)
}

if(optionExample == "m") {
    // Examples should be merged into the regular modules and then the example folders removed
    // Delete core source folder (if exists) and then rename core example source to core source
    if(coreSrc.exists()) {
        FileUtils.deleteDirectory(coreSrc)
    }
    if(coreExampleBundle.exists()) {
        assert coreExampleSrc.renameTo(coreSrc);
        FileUtils.deleteDirectory(coreExampleBundle)
    }
    removeModule(rootPom, "core.example")
    // Delete ui.apps source folder (if exists) and then rename ui.apps example source to ui.apps source
    if(uiAppsSrc.exists()) {
        FileUtils.deleteDirectory(uiAppsSrc)
    }
    if(uiAppsExamplePackage.exists()) {
        assert uiAppsExampleSrc.renameTo(uiAppsSrc);
        FileUtils.deleteDirectory(uiAppsExamplePackage)
    }
    removeModule(rootPom, "ui.apps.example")
} else if(optionExample == "d") {
    // Examples should be deleted
    // Remove core.example
    if(coreExampleBundle.exists()) {
        FileUtils.deleteDirectory(coreExampleBundle)
    }
    removeModule(rootPom, "core.example")
    // Remove ui.apps.example
    if(uiAppsExamplePackage.exists()) {
        FileUtils.deleteDirectory(uiAppsExamplePackage)
    }
    removeModule(rootPom, "ui.apps.example")
}
