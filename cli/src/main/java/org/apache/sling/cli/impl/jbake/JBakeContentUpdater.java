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
package org.apache.sling.cli.impl.jbake;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JBakeContentUpdater {
    
    private static final Pattern DOWNLOAD_LINE_PATTERN = Pattern.compile("^.*\"([a-zA-Z\\s\\-]+)\\|([a-zA-Z\\.\\-]+)\\|([0-9\\.\\-]+).*$");

    public int updateDownloads(Path downloadsTemplatePath, String newReleaseName, String newReleaseVersion) throws IOException {
        
        int[] changeCount = new int[1];
        
        List<String> updatedLines = Files.readAllLines(downloadsTemplatePath, StandardCharsets.UTF_8).stream()
                .map(line -> {
                    Matcher matcher = DOWNLOAD_LINE_PATTERN.matcher(line);
                    if ( !matcher.find() )
                        return line;
                    
                    if ( ! matcher.group(1).equals(newReleaseName) )
                        return line;
                    
                    changeCount[0]++;
                    
                    StringBuilder buffer = new StringBuilder();
                    buffer.append(line.substring(0, matcher.start(3)));
                    buffer.append(newReleaseVersion);
                    buffer.append(line.substring(matcher.end(3)));
                    
                    return buffer.toString();
                }).collect(Collectors.toList());

        Files.write(downloadsTemplatePath, updatedLines);
        
        return changeCount[0];
    }

    public void updateReleases(Path releasesPath, String releaseName, String releaseVersion, LocalDateTime releaseTime) throws IOException {
        
        List<String> releasesLines = Files.readAllLines(releasesPath, StandardCharsets.UTF_8);
        String dateHeader = "## " + releaseTime.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH));
        
        int releaseLineIdx = -1;
        int dateLineIdx = -1;
        for ( int i = 0 ; i < releasesLines.size(); i++ ) {
            String releasesLine = releasesLines.get(i);
            if ( releasesLine.startsWith("This is a list of all our releases") ) {
                releaseLineIdx = i;
            }
            if ( releasesLine.equals(dateHeader) ) {
                dateLineIdx = i;
            }
        }
        
        if ( dateLineIdx == -1 ) {
            // need to add month marker
            releasesLines.add(releaseLineIdx + 1, "");
            releasesLines.add(releaseLineIdx + 2, dateHeader);
            releasesLines.add(releaseLineIdx + 3, "");
            dateLineIdx = releaseLineIdx + 2;
        }
        
        String date = formattedDay(releaseTime);
        
        // inspect all lines in the current month ( until empty line found )
        // to see if the release date already exists
        boolean changed = false;
        for ( int i = dateLineIdx +2 ; i < releasesLines.size(); i++ ) {
            String potentialLine = releasesLines.get(i);
            if ( potentialLine.trim().isEmpty() )
                break;
            
            if ( potentialLine.endsWith("(" +date+")") ) {
                if ( potentialLine.contains(releaseName + " " + releaseVersion ) ) {
                    changed = true;
                    break;
                }
                
                int insertionIdx = potentialLine.indexOf('(') - 1;
                StringBuilder buffer = new StringBuilder();
                buffer
                    .append(potentialLine.substring(0, insertionIdx))
                    .append(", ")
                    .append(releaseName)
                    .append(' ')
                    .append(releaseVersion)
                    .append(' ')
                    .append(potentialLine.substring(insertionIdx + 1));
                
                releasesLines.set(i, buffer.toString());
                changed = true;
                break;
            }
        }
        
        if ( !changed )
            releasesLines.add(dateLineIdx + 2, "* " + releaseName + " " + releaseVersion +" (" + date + ")");

        Files.write(releasesPath, releasesLines);
    }
    
    private String formattedDay(LocalDateTime releaseTime) {
        String date = releaseTime.format(DateTimeFormatter.ofPattern("d", Locale.ENGLISH));
        switch (date) {
        case "1":
            return "1st";
        case "2":
            return "2nd";
        case "3":
            return "3rd";
        default:
            return date + "th";
        }
    }
}
