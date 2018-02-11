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
package org.apache.sling.resource.stream.predicates;

import java.util.Objects;
/*
 * Copyright 2016 Jason E Bailey
 *
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
import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;

/**
 * Predicates that are to be used against a child resource
 * 
 *
 */
public class ChildResourcePredicates {

	private final String name;

	private ChildResourcePredicates(String name) {
		this.name = Objects.requireNonNull(name, "name value may not be null");
		;
	}

	/**
	 * Instantiates a ChildResourcePredicate object to provide the application of
	 * Predicates against the named child
	 * 
	 * @param name
	 *            of the expected child resource
	 * @return Object providing helper predicates for a child resource
	 */
	static public ChildResourcePredicates child(String name) {
		return new ChildResourcePredicates(name);
	}

	/**
	 * Applies a predicate against the named child resource. The returned predicate
	 * will always return 'false' for a child that doesn't exist
	 * 
	 * @param predicate
	 *            to be used against the child resource
	 * @return Predicate which will apply the given predicate to the child resource
	 */
	public Predicate<Resource> has(Predicate<Resource> predicate) {
		Objects.requireNonNull(predicate, "predicate may not be null");
		return resource -> {
			Resource child = resource.getChild(name);
			if (child != null) {
				return predicate.test(child);
			}
			return false;
		};
	}

}
