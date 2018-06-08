/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.resource.filter;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.filter.ResourceFilter;
import org.apache.sling.resource.filter.ResourceStream;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceFilterArgTest {

    private static final String START_PATH = "/content/sample/en";
    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void setUp() throws ParseException {
        context.load().json("/data.json", "/content/sample/en");
    }

    @Test
    public void testMatchingNameInArg() throws Exception {
        ResourceFilter filter = new ResourceFilter("[jcr:content/jcr:title] == $lang")
                .addArgument("date", "2013-08-08T16:32:59").addArgument("lang", "Mongolian");
        List<Resource> found = handle(START_PATH, filter);
        assertEquals(1, found.size());
    }

    @Test
    public void testMatchingNameAndMultipleArgs() throws Exception {
        ResourceFilter filter = new ResourceFilter("[jcr:content/created] > $date and [jcr:content/jcr:title] == $lang")
                .addArgument("date", "2013-08-08T16:32:59").addArgument("lang", "Mongolian");
        List<Resource> found = handle(START_PATH, filter);
        assertEquals(1, found.size());
    }

    @Test
    public void testNameFunctionAgainstRegex() throws ParseException, Exception {
        ResourceFilter query = new ResourceFilter("name() like $regex").addArgument("regex", "testpage[1-2]");
        List<Resource> found = handle(START_PATH, query);
        assertEquals(2, found.size());
    }

    private List<Resource> handle(String startPath, ResourceFilter filter) {
        Resource resource = context.resourceResolver().getResource(startPath);
        return new ResourceStream(resource).stream(r -> true).filter(filter).collect(Collectors.toList());
    }

}
