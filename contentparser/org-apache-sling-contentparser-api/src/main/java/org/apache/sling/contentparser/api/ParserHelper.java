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

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public final class ParserHelper {

    public static final String ECMA_DATE_FORMAT = "EEE MMM dd yyyy HH:mm:ss 'GMT'Z";
    public static final String ISO_8601_MILLISECONDS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSVV";
    public static final Locale DATE_FORMAT_LOCALE = Locale.US;
    public static final DateTimeFormatter ECMA_DATE_FORMATTER = DateTimeFormatter.ofPattern(ECMA_DATE_FORMAT, DATE_FORMAT_LOCALE);
    public static final DateTimeFormatter ISO_8601_MILLISECONDS_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(ISO_8601_MILLISECONDS_DATE_FORMAT,
                    DATE_FORMAT_LOCALE);

    /**
     * Attempts to parse a {@code string} using first the {@link #ISO_8601_MILLISECONDS_DATE_FORMAT} format and then the {@link
     * #ECMA_DATE_FORMAT}.
     *
     * @param string the string to parse
     * @return a {@link Calendar} containing the parsed date or {@code null}, if the parsing failed
     */
    public static Calendar parseDate(String string) {
        Calendar calendar = Calendar.getInstance();
        try {
            final OffsetDateTime offsetDateTime = OffsetDateTime.parse(string, ISO_8601_MILLISECONDS_DATE_FORMATTER);
            final Instant instant = offsetDateTime.toInstant();
            calendar.setTime(Date.from(instant));
            calendar.setTimeZone(TimeZone.getTimeZone(offsetDateTime.getOffset()));
        } catch (DateTimeParseException e) {
            try {
                final OffsetDateTime offsetDateTime = OffsetDateTime.parse(string, ECMA_DATE_FORMATTER);
                final Instant instant = offsetDateTime.toInstant();
                calendar.setTime(Date.from(instant));
                calendar.setTimeZone(TimeZone.getTimeZone(offsetDateTime.getOffset()));
            } catch (DateTimeParseException ee) {
                calendar = null;
            }
        }
        return calendar;
    }

    /**
     * Converts a multi-value property to a single object.
     *
     * @param values the multi-value property's values
     * @return an object representation of the multi-value property
     */
    public static Object convertSingleTypeArray(Object[] values) {
        if (values.length == 0) {
            return values;
        }
        Class<?> itemType = null;
        for (Object value : values) {
            if (value == null) {
                throw new ParseException("Multi-value array must not contain null values.");
            }
            if (value instanceof Map) {
                throw new ParseException("Multi-value array must not contain maps/objects.");
            }
            if (itemType == null) {
                itemType = value.getClass();
            } else if (itemType != value.getClass()) {
                throw new ParseException("Multivalue array must not contain values with different types "
                        + "(" + itemType.getName() + ", " + value.getClass().getName() + ").");
            }
        }
        Object convertedArray = Array.newInstance(itemType, values.length);
        for (int i = 0; i < values.length; i++) {
            Array.set(convertedArray, i, values[i]);
        }
        return convertedArray;
    }

}
