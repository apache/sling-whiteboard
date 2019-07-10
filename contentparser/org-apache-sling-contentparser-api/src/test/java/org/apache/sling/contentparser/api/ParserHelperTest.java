/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.contentparser.api;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class ParserHelperTest {

    @Test
    public void parseDate() {
        Map<String, int[]> formats = new HashMap<>();
        formats.put("Sun Oct 31 2010 21:48:04 GMT+0100", new int[]{31, 10, 2010, 21, 48, 4, 0, 1});
        formats.put("Sun Oct 31 2010 21:48", null);
        formats.put("2014-09-19T21:20:26.812+02:00", new int[]{19, 9, 2014, 21, 20, 26, 812, 2});
        formats.put("2014-09-19T21:20:26.812", null);
        for (Map.Entry<String, int[]> entry : formats.entrySet()) {
            int[] dateAsInts = entry.getValue();
            Calendar calendar = ParserHelper.parseDate(entry.getKey());
            if (dateAsInts == null) {
                assertNull("Expected a null return value for string " + entry.getKey(), calendar);
            } else {
                assertEquals(dateAsInts[0], calendar.get(Calendar.DAY_OF_MONTH));
                assertEquals(dateAsInts[1], calendar.get(Calendar.MONTH) + 1);
                assertEquals(dateAsInts[2], calendar.get(Calendar.YEAR));
                assertEquals(dateAsInts[3], calendar.get(Calendar.HOUR_OF_DAY));
                assertEquals(dateAsInts[4], calendar.get(Calendar.MINUTE));
                assertEquals(dateAsInts[5], calendar.get(Calendar.SECOND));
                assertEquals(dateAsInts[6], calendar.get(Calendar.MILLISECOND));
                assertEquals(dateAsInts[7], calendar.getTimeZone().getRawOffset() / 3600 / 1000);

            }
        }
    }
}
