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

import java.lang.reflect.Array;
import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * Provides property based predicates. The method follows the following
 * conventions
 * 
 * <br>
 * is = equal to argument<br>
 * isNot = not equal to argument<br>
 * isIn = property is a single value which matches one of the arguments passed in
 * for comparison<br>
 * greaterThan = greater than the property<br>
 * lessThan = less than the property<br>
 * exists = is the property of the child <br> 
 * contains = property is an array which contains all of the arguments passed in<br>
 * 
 *
 */
public class PropertyPredicates {

	// key value to be used against the provided resource object
	private final String key;

	private PropertyPredicates(String name) {
		this.key = Objects.requireNonNull(name, "value may not be null");;
	}

	/**
	 * Used to define which value in the underlying map will we be used.
	 * 
	 * @param name key value of the property
	 * @return PropertyPredicate instance
	 */
	static public PropertyPredicates property(String name) {
		return new PropertyPredicates(name);
	}
	
	/**
	 * Assumes that the referenced property value is a date and that this date
	 * is earlier in the epoch than the one being tested
	 * 
	 * @param when latest acceptable time
	 * @return predicate which will perform the matching
	 */
	public Predicate<Resource> isBefore(Date when) {
		Objects.requireNonNull(when, "value may not be null");
		return value -> {
			Date then = value.adaptTo(ValueMap.class).get(key, Date.class);
			if (then != null) {
				return then.before(when);
			}
			return false;
		};
	}

	/**
	 * Assumes that the referenced property value is a date and that this date
	 * is later in the epoch than the one being tested
	 * 
	 * @param when earliest acceptable value
	 * @return predicate
	 */
	public Predicate<Resource> isAfter(Date when) {
		Objects.requireNonNull(when, "value may not be null");
		return value -> {
			Date then = value.adaptTo(ValueMap.class).get(key, Date.class);
			if (then != null) {
				return then.after(when);
			}
			return false;
		};
	}

	/*
	 * Generic equalities method that is accessed via public methods that have
	 * specific types
	 */
	public <T> Predicate<Resource> is(T type) {
		Objects.requireNonNull(type, "type value may not be null");
		return resource -> {
			@SuppressWarnings("unchecked")
			T propValue = (T) valueMapOf(resource).get(key,
					type.getClass());
			return type.equals(propValue);
		};

	}
	
	/*
	 * Generic greater then method that is accessed via public methods that have
	 * specific types
	 */
	public <T extends Comparable<T>> Predicate<Resource> greaterThan(T type) {
		Objects.requireNonNull(type, "type value may not be null");
		return resource -> {
			@SuppressWarnings("unchecked")
			T propValue = (T) valueMapOf(resource).get(key,
					type.getClass());
			if (propValue instanceof Comparable<?>){
				return ((Comparable<T>)propValue).compareTo(type) > 0;
			}
			return type.equals(propValue);
		};

	}
	
	/*
	 * Generic greater then method that is accessed via public methods that have
	 * specific types
	 */
	public <T extends Comparable<T>> Predicate<Resource> greaterThanOrEqual(T type) {
		Objects.requireNonNull(type, "type value may not be null");
		return resource -> {
			@SuppressWarnings("unchecked")
			T propValue = (T) valueMapOf(resource).get(key,
					type.getClass());
			if (propValue instanceof Comparable<?>){
				return ((Comparable<T>)propValue).compareTo(type) >= 0;
			}
			return type.equals(propValue);
		};

	}
	
	/*
	 * Generic greater then method that is accessed via public methods that have
	 * specific types
	 */
	public <T extends Comparable<T>> Predicate<Resource> lessThan(T type) {
		Objects.requireNonNull(type, "type value may not be null");
		return resource -> {
			@SuppressWarnings("unchecked")
			T propValue = (T) valueMapOf(resource).get(key,
					type.getClass());
			if (propValue instanceof Comparable<?>){
				return ((Comparable<T>)propValue).compareTo(type) < 0;
			}
			return type.equals(propValue);
		};

	}
	
	/*
	 * Generic greater then method that is accessed via public methods that have
	 * specific types
	 */
	public <T extends Comparable<T>> Predicate<Resource> lessThanOrEqual(T type) {
		Objects.requireNonNull(type, "type value may not be null");
		return resource -> {
			@SuppressWarnings("unchecked")
			T propValue = (T) valueMapOf(resource).get(key,
					type.getClass());
			if (propValue instanceof Comparable<?>){
				return ((Comparable<T>)propValue).compareTo(type) >= 0;
			}
			return type.equals(propValue);
		};

	}

	public <T> Predicate<Resource> isNot(final T type) {
		return is(type).negate();
	}

	@SuppressWarnings("unchecked")
	public <T> Predicate<Resource> contains(final T[] values) {
		Objects.requireNonNull(values, "value may not be null");
		return resource -> {
			T[] propValues = (T[]) valueMapOf(resource).get(key,
					values.getClass());
			if (propValues == null) {
				if (values.length > 1) {
					// no point converting if the number of values to test
					// exceeds the possible values in the repository
					return false;
				}
				Class<?> componentType = values.getClass().getComponentType();
				T tempValue = (T) valueMapOf(resource).get(key,
						componentType);
				if (tempValue != null) {
					propValues = (T[]) Array.newInstance(componentType, 1);
					propValues[0] = tempValue;
				}
			}
			// property identified by resource is either not present or is
			// of a type that is not the type being requested
			if (propValues == null || propValues.length < values.length) {
				return false;
			}
			// validate that all items in values have matches in properties
			for (T item : values) {
				innerloop: {
					for (T propItem : propValues) {
						if (item.equals(propItem)) {
							break innerloop;
						}
					}
					return false;
				}
			}
			return true;
		};

	}
	
	/**
	 * Contains any take one or move values as it's input and returns true if any of
	 * the property values matches any of the provides values.
	 * 
	 * @param values
	 *            One or more objects to be compared with
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Predicate<Resource> containsAny(final T... values) {
		Objects.requireNonNull(values, "value may not be null");
		return resource -> {
			T[] propValues = (T[]) valueMapOf(resource).get(key, values.getClass());
			if (propValues == null) {
				if (values.length > 1) {
					// no point converting if the number of values to test
					// exceeds the possible values in the repository
					return false;
				}
				Class<?> componentType = values.getClass().getComponentType();
				T tempValue = (T) valueMapOf(resource).get(key, componentType);
				if (tempValue != null) {
					propValues = (T[]) Array.newInstance(componentType, 1);
					propValues[0] = tempValue;
				}
			}
			// property identified by resource is not present
			if (propValues == null) {
				return false;
			}
			// validate that all items in values have matches in properties
			for (T item : values) {
				for (T propItem : propValues) {
					if (item.equals(propItem)) {
						return true;
					}
				}
			}
			return false;
		};

	}


	public <T> Predicate<Resource> isIn(final T[] values) {
		Objects.requireNonNull(values, "values may not be null");
		return resource -> {
			Object propValue = valueMapOf(resource).get(key,
					values.getClass().getComponentType());
			if (propValue == null) {
				return false;
			}
			for (T value : values) {
				if (value.equals(propValue)) {
					return true;
				}
			}
			return false;
		};
	}

	/**
	 * property value is not null
	 * 
	 * @return a predicate that determines existence of the value
	 */
	public Predicate<Resource> exists() {
		return resource -> valueMapOf(resource).get(key) != null;
	}

	/**
	 * property value is null
	 * 
	 * @return a predicate that determines non existence of the value
	 */
	public Predicate<Resource> doesNotExist() {
		return exists().negate();
	}

	
	public Predicate<Resource> isNotIn(final Object... objects) {
		Objects.requireNonNull(objects, "objects may not be null");
		return resource -> {
			Object value = valueMapOf(resource).get(key);

			for (Object object : objects) {
				if (object.equals(value)) {
					return false;
				}
			}
			return true;
		};
	}
	
	private ValueMap valueMapOf(Resource resource){
		if (resource == null || ResourceUtil.isNonExistingResource(resource)){
			return ValueMap.EMPTY;
		}
		 return resource.adaptTo(ValueMap.class);
	}
	
}
