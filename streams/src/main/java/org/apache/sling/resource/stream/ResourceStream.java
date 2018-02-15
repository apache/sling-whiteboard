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
import java.util.function.Consumer;
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
	
	private Predicate<Resource> branchSelector = resource -> true;

	private Predicate<Resource> childSelector = resource -> true;

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
	 * Resets the starting path for the query to be the provided path. This can be
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
	 * Sets the branch selector for the traversal process. 
	 * 
	 * @param path
	 *            set the internal resource to path
	 * @return this locator
	 * @throws ParseException 
	 */
	public ResourceStream setBranchSelector(String branchSelector) throws ParseException {
		return setBranchSelector(new ResourceFilter(branchSelector));
	}
	
	/**
	 * Sets the branch selector for the traversal process. 
	 * 
	 * @param path
	 *            set the internal resource to path
	 * @return this locator
	 */
	public ResourceStream setBranchSelector(Predicate<Resource> branchSelector) {
		this.branchSelector = Objects.requireNonNull(branchSelector);
		return this;
	}
	
	/**
	 * Sets the branch selector for the traversal process. 
	 * 
	 * @param path
	 *            set the internal resource to path
	 * @return this locator
	 * @throws ParseException 
	 */
	public ResourceStream setChildSelector(String childSelector) throws ParseException {
		return setChildSelector(new ResourceFilter(childSelector));
	}
	
	/**
	 * Sets a child selector which defines whether a given reosource should be part of the stream 
	 * 
	 * @param path
	 *            set the internal resource to path
	 * @return this locator
	 */
	public ResourceStream setChildSelector(Predicate<Resource> childSelector) {
		this.childSelector = Objects.requireNonNull(childSelector);
		return this;
	}

	/**
	 * Provides a stream of resources starting from the initiator resource and
	 * traversing through it's descendants
	 * 
	 * @return self closing {@code Stream<Resource>} of unknown size.
	 */
	public Stream<Resource> stream() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Resource>() {

			private final LinkedList<Resource> resourcesToCheck = new LinkedList<>();

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
				} while (startOfRange > 0 || !childSelector.test(current));
				return true;
			}

			@Override
			public Resource next() {
				return current;
			}
		}, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
	}

	/**
	 * Sets a child selector which defines whether a given reosource should be part of the stream 
	 * 
	 * @param path
	 *            set the internal resource to path
	 * @return this locator
	 */
	public void forEach(Consumer<Resource> consumer) {
		stream().forEach(consumer);
	}
}
