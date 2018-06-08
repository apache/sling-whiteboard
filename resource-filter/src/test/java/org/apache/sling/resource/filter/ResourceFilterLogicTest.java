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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.filter.ResourceFilterStream;
import org.apache.sling.resource.filter.impl.ParseException;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceFilterLogicTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private static String START_PATH = "/content/sample/en";

    @Before
    public void setUp() throws ParseException, java.text.ParseException {
        context.load().json("/data.json", START_PATH);
    }

    @Test
    public void testDateAndProperty() throws ParseException {
        String query = "[jcr:content/created] > '2013-08-08T16:32:59' and [jcr:content/jcr:title] == 'Mongolian'";
        List<Resource> found = handle(START_PATH, query);
        assertEquals(1, found.size());

        query = "([jcr:content/created] < '2013-08-08T16:32' and [jcr:content/jcr:title] == 'English') or [jcr:content/jcr:title] == 'Mongolian'";
        found = handle(START_PATH, query);
        assertEquals(4, found.size());

        query = "[jcr:content/created] < '2013-08-08T16:32' and ([jcr:content/jcr:title] == 'English' or [jcr:content/jcr:title] == 'Mongolian')";
        found = handle(START_PATH, query);
        assertEquals(3, found.size());

        query = "[jcr:content/created] < date('2013-08-08T16:32:59.000+02:00') or [jcr:content/jcr:title] == 'Mongolian'";
        found = handle(START_PATH, query);
        assertEquals(4, found.size());
    }

    private List<Resource> handle(String path, String filter) throws ParseException {
        Resource resource = context.resourceResolver().getResource(path);
        return new ResourceFilterStream(resource).stream(r -> true).filter(new ResourceFilter(filter))
                .collect(Collectors.toList());
    }
}
