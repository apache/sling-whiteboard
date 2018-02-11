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
import java.util.function.Predicate;

/**
 * A collection of terms to assist in building a fluent api. These terms may not
 * have functionality other than providing syntax sugar over existing features,
 * or merely providing context for other terms
 *
 */
public class Conditions {

	/**
	 * Syntactic sugar to provide context for the builder
	 * 
	 * @param predicate
	 *            to be wrapped
	 * @return the predicate that was passed in
	 */
	public static <T> Predicate<T> where(Predicate<T> predicate) {
		return predicate;
	}

	/**
	 * Syntactic sugar to provide context for the builder
	 * 
	 * @param predicate
	 *            to be wrapped
	 * @return the predicate that was passed in
	 */
	public static <T> Predicate<T> when(Predicate<T> predicate) {
		return predicate;
	}

	/**
	 * 'or' equivalent. only evaluates the second predicate if the first one
	 * fails
	 * 
	 * @param firstPredicate
	 *            always evaluated
	 * @param secondPredicate
	 *            evaluated if firstPredicate returns false
	 * @return a new predicate which wraps the or method on the firstPredicate
	 */
	public static <T> Predicate<T> either(
			final Predicate<T> firstPredicate,
			final Predicate<T> secondPredicate) {
		Objects.requireNonNull(firstPredicate, "predicate may not be null");
		Objects.requireNonNull(secondPredicate, "predicate may not be null");
		return firstPredicate.or(secondPredicate);
	}

}
