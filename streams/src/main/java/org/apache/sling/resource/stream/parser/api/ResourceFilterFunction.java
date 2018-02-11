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
package org.apache.sling.resource.stream.parser.api;

import java.util.List;
import java.util.function.Function;

import org.apache.sling.api.resource.Resource;

/**
 * A ResourceFilterFunction implementation is used to translate a String from a script or
 * an Object as a result of a custom function into a value that is used for
 * Comparison
 * 
 */
public interface ResourceFilterFunction {

	/**
	 * This method returns a {@code Function} which accepts the resource being
	 * tested and returns an Object to be used as part of a comparison.
	 * 
	 * @param arguments
	 *            A list of {@code Function}'s which provides the arguments defined
	 *            in the script, to obtain the arguments, each argument must be
	 *            called
	 * @return A {@code Function} which will provide a String, Instant, or Number to
	 *         be used as part of a comparison or Function
	 */
	Function<Resource, Object> provision(List<Function<Resource, Object>> arguments);

}
