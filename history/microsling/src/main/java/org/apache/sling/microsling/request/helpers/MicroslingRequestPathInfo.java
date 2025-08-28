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
package org.apache.sling.microsling.request.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.sling.microsling.api.Resource;
import org.apache.sling.microsling.api.ResourceMetadata;
import org.apache.sling.microsling.api.SlingRequestPathInfo;

/** microsling request URI parser that provides SlingRequestPathInfo 
 *  for the current request, based on the path of the Resource.
 *  
 *  The values provided by this depend on the Resource.getURI() value,
 *  as the ResourceResolver might use all or only part of the request 
 *  URI path to locate the resource (see also SLING-60 ).
 *  
 *  What we're after is the remainder of the path, the part that was
 *  not used to locate the Resource, and we split that part in different
 *  subparts: selectors, extension and suffix.
 *  
 *  @see MicroslingRequestPathInfoTest for a number of examples.
 */
public class MicroslingRequestPathInfo implements SlingRequestPathInfo {
    
    private final String unusedContentPath;
    private final String selectorString;
    private final String [] selectors;
    private final String extension;
    private final String suffix;
    private final String resourcePath;
    
    private final static String EMPTY = "";
    
    private static Pattern splitInThreePattern;
    private static Pattern selectorExtPattern; 
    private static PatternSyntaxException parsingPatternException;
    
    static {
        try {
            // we love regular expressions (along with their unit tests ;-)
            // first pattern splits (path) (selectors + extension) (suffix) 
            splitInThreePattern = Pattern.compile("([^\\.]*)?((\\.[^/]*)?(/.*)?)?");
            
            // second pattern separates (selectors) and (extension)
            selectorExtPattern = Pattern.compile("(\\.(.*))?(\\.([^\\.]*))");
            
            parsingPatternException = null;
        } catch(PatternSyntaxException pse) {
            parsingPatternException = pse;
        }
    }
    
    /** break requestPath as required by SlingRequestPathInfo */
    public MicroslingRequestPathInfo(Resource r,String requestPath) throws PatternSyntaxException {
        if(parsingPatternException != null) {
            throw parsingPatternException;
        }
        
        String pathToParse = requestPath;
        if(pathToParse == null) {
            pathToParse = "";
        }
        
        if(r==null) {
            resourcePath = null;
        } else {
            resourcePath = (String)r.getMetadata().get(ResourceMetadata.RESOLUTION_PATH);
            if(resourcePath!=null && pathToParse.length() >= resourcePath.length()) {
                pathToParse = pathToParse.substring(resourcePath.length());
            }
        }
        
        Matcher m = splitInThreePattern.matcher(pathToParse);
        unusedContentPath = groupOrEmpty(m,1);
        suffix = groupOrEmpty(m,4);
        
        final String selAndExt = groupOrEmpty(m,3);
        m = selectorExtPattern.matcher(selAndExt);
        selectorString = groupOrEmpty(m,2);
        selectors = selectorString.split("\\.");
        extension = groupOrEmpty(m,4);
    }
    
    /** Return the contents of m.group(index), empty string if that's null */
    private static String groupOrEmpty(Matcher m, int index) {
        String result = null;
        if(m.matches()) {
            result = m.group(index);
        }
        return result == null ? EMPTY : result;
    }
    
    @Override
    public String toString() {
        return 
          "SlingRequestPathInfoParser:"
          + ", path='" + unusedContentPath + "'"
          + ", selectorString='" + selectorString + "'"
          + ", extension='" + extension + "'"
          + ", suffix='" + suffix + "'"
        ;
    }
    
    public String getExtension() {
        return extension;
    }

    public String getSelector(int i) {
        if(i >= 0 && i < selectors.length) {
            return selectors[i];
        }
        return null;
    }

    public String[] getSelectors() {
        return selectors;
    }

    public String getSelectorString() {
        return selectorString;
    }

    public String getSuffix() {
        return suffix;
    }
    
    public String getUnusedContentPath() {
        return unusedContentPath;
    }

    public String getResourcePath() {
        return resourcePath;
    }
}
