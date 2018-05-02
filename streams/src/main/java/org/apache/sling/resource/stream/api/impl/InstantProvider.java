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
package org.apache.sling.resource.stream.api.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.stream.api.FilterFunction;

/**
 * Implementation of {@link FilterFunction} for the 'date' function.
 * 
 * The following combination of arguments are supported
 * 
 * <pre>
 * arguments      results
 * ======================================================
 * none     | current system time
 * one      | ISO88601 String with offset
 * two      | Date String followed by Date Format String
 * 
 * </pre>
 *
 */
public class InstantProvider implements FilterFunction {

	@Override
	public Object apply(List<Function<Resource, Object>> arguments, Resource resource) {
		if (arguments.isEmpty()) {
			return Instant.now();
		}
		String dateString = arguments.get(0).apply(resource).toString();
		String formatString = null;
		if (arguments.size() > 1) {
			formatString = arguments.get(1).apply(resource).toString();
			SimpleDateFormat dateFormat = new SimpleDateFormat(formatString);
			try {
				return Instant.ofEpochMilli(dateFormat.parse(dateString).getTime());
			} catch (ParseException e) {
				return null;
			}
		} else {
			return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateString, OffsetDateTime::from).toInstant();
		}

	}
}
