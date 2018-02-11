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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.stream.parser.ParseException;

/**
 * Base class from which a fluent api can be created or which can be defined
 * using the integrated query language.
 * 
 * Additionally provides the ability to stream child resources.
 *
 */
public class ResourceStream {

	// starting resource
	private Resource resource;

	private long limit = 0;

	private long startOfRange;

	/**
	 * Starting point to locate resources. resources of the start resource.
	 * 
	 * @param resource
	 *            starting point for the traversal
	 * @return new instance of ResourceQuery;
	 */
	public static ResourceStream from(@Nonnull Resource resource) {
		return new ResourceStream(resource);
	}

	/*
	 * Constructor to establish the starting resource
	 * 
	 * @param resource
	 */
	private ResourceStream(Resource resource) {
		this.resource = Objects.requireNonNull(resource);
	}

	/**
	 * Rests the starting path for the query to be the provided path. This can be
	 * used to limit the possible branching options beneath a resource tree, or to
	 * use a parent the resource as the basis of permissions for another resource
	 * structure
	 * 
	 * 
	 * @param path
	 *            set the internal resource to path
	 * @return this locator
	 */
	public ResourceStream startingFrom(String path) {
		this.resource = Objects.requireNonNull(resource.getResourceResolver().getResource(path));
		return this;
	}

	/**
	 * Sets the maximum number of items to be returned or processed. Starting from
	 * the first matching resource. This method is mutually exclusive to the range
	 * method
	 * 
	 * This performs the same form of limitation as a limit on a Stream
	 * 
	 * @param number
	 *            maximum number of items to be returned
	 * @return this locator
	 */
	public ResourceStream limit(long number) {
		if (number < 0) {
			throw new IllegalArgumentException("value may not be negative");
		}
		this.startOfRange = 0;
		this.limit = number;
		return this;
	}

	/**
	 * Sets the maximum number of items to be returned or processed. Starting from
	 * the nth identified resource as set by the startOfRange. This method is
	 * mutually exclusive to the limit method
	 * 
	 * This can be achieved on a Stream by performing a a filter operation
	 * 
	 * @param startOfRange
	 * @param limit
	 * @return
	 */
	public ResourceStream range(long startOfRange, long limit) {
		if (startOfRange < 0 || limit < 0) {
			throw new IllegalArgumentException("value may not be negative");
		}
		this.startOfRange = startOfRange;
		this.limit = limit;
		return this;
	}

	/**
	 * Provides a stream of resources starting from the initiator resource and
	 * traversing through it's descendants The only fluent api check it performs is
	 * of the traversal predicate.
	 * 
	 * @return self closing {@code Stream<Resource>} of unknown size.
	 */
	public Stream<Resource> stream() {
		return stream(resource -> true);
	}

	/**
	 * Provides a stream of resources starting from the initiator resource and
	 * traversing through it's descendants The only fluent api check it performs is
	 * of the traversal predicate.
	 * 
	 * @return self closing {@code Stream<Resource>} of unknown size.
	 */
	public Stream<Resource> stream(String branchSelector) throws ParseException {
		return stream(new ResourceFilter(branchSelector));
	}

	/**
	 * Provides a stream of resources starting from the initiator resource and
	 * traversing through it's descendants The only fluent api check it performs is
	 * of the traversal predicate.
	 * 
	 * @return self closing {@code Stream<Resource>} of unknown size.
	 */
	public Stream<Resource> stream(Predicate<Resource> branchSelector) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Resource>() {

			final LinkedList<Resource> resourcesToCheck = new LinkedList<>();
			final AtomicInteger index = new AtomicInteger(0);

			{
				resourcesToCheck.addFirst(resource);
			}

			Resource current;

			@Override
			public boolean hasNext() {
				do {
					if (resourcesToCheck.isEmpty()) {
						return false;
					}

					current = resourcesToCheck.removeFirst();

					current.listChildren().forEachRemaining(child -> {
						if (branchSelector.test(child)) {
							resourcesToCheck.add(index.getAndIncrement(), child);
						}
					});

					index.set(0);

					if (startOfRange > 0) {
						--startOfRange;
					}
					if (limit > 0 && startOfRange == 0) {
						if (--limit == 0) {
							resourcesToCheck.clear();
						}
					}
				} while (startOfRange > 0);
				return true;
			}

			@Override
			public Resource next() {
				return current;
			}
		}, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
	}

}
