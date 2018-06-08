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
package org.apache.sling.resource.stream;

import static org.apache.sling.resource.predicates.ChildResourcePredicates.child;
import static org.apache.sling.resource.predicates.PropertyPredicates.property;
import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourcePredicateTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private Calendar midPoint;

    private static String DATE_STRING = "Thu Aug 07 2013 16:32:59 GMT+0200";

    private static String DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";

    private List<Resource> list;

    @Before
    public void setUp() throws ParseException {
        context.load().json("/data.json", "/content/sample/en");
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT+0200"));
        cal.setTime(new SimpleDateFormat(DATE_FORMAT).parse(DATE_STRING));
        midPoint = cal;
        Resource resource = context.resourceResolver().getResource("/content/sample/en");
        list = new ArrayList<>();
        resource.listChildren().forEachRemaining(list::add);
    }

    @Test
    public void testObtainResourceFromContext() {
        Resource resource = context.resourceResolver().getResource("/content/sample/en");
        assertEquals("en", resource.getName());
    }

    @Test
    public void testMatchingName() {
        List<Resource> found = list.stream().filter(item -> {
            return item.getName().equals("testpage1");
        }).collect(Collectors.toList());
        assertEquals(1, found.size());
    }

    @Test
    public void testBeforeThenDate() {
        List<Resource> found = list.stream()
                .filter(property("jcr:content/created").isBefore(Calendar.getInstance()))
                .collect(Collectors.toList());
        assertEquals(7, found.size());
    }

    @Test
    public void testAfterThenDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        List<Resource> found = list.stream().filter(child("jcr:content").has(property("created").isAfter(cal)))
                .collect(Collectors.toList());
        assertEquals(7, found.size());
    }

    @Test
    public void testAfterMidDate() {
        List<Resource> found = list.stream().filter(property("jcr:content/created").isAfter(midPoint))
                .collect(Collectors.toList());
        assertEquals(4, found.size());
    }

    @Test
    public void testBeforeMidDate() {
        List<Resource> found = list.stream().filter(property("jcr:content/created").isBefore(midPoint))
                .collect(Collectors.toList());
        assertEquals(1, found.size());
    }

}
