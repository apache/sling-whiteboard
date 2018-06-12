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
package org.apache.sling.resource.filter;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;

/**
 * Utility to create a Stream<Resource> of Resource objects from a managed
 * traversal of a Resource tree
 *
 */
public class ResourceStream {

    private Resource resource;

    public ResourceStream(Resource resource) {
        this.resource = resource;
    }

    /**
     * Provides a depth first {@code Stream<Resource>} traversal of the resource
     * tree starting with the current resource. The traversal is controlled by the
     * provided predicate which determines if a given child is traversed. If no
     * children matches the predicate, the traversal for that branch ends
     * 
     * @param branchSelector
     *            used to determine whether a given child resource is traversed
     * @return {@code Stream<Resource>} of unknown size.
     */
    public Stream<Resource> stream(Predicate<Resource> branchSelector) {

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Resource>() {

            private final Deque<Iterator<Resource>> resources = new LinkedList<>();
            private Resource current = resource;

            {
                resources.push(resource.getChildren().iterator());
            }

            @Override
            public boolean hasNext() {
                if (current == null) {
                    return seek();
                }
                return true;
            }

            @Override
            public Resource next() {
                if (current == null) {
                    seek();
                }
                if (current == null) {
                    throw new NoSuchElementException();
                }
                Resource next = current;
                current = null;
                return next;
            }

            private boolean seek() {
                while (!resources.isEmpty()) {
                    Iterator<Resource> iterator = resources.peek();
                    if (iterator.hasNext()) {
                        current = iterator.next();
                        if (branchSelector.test(current)) {
                            resources.push(current.getChildren().iterator());
                        }
                        return true;
                    }
                    resources.pop();
                }
                return false;
            }

        }, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }

    public Stream<Resource> listChildren(Predicate<Resource> childSelector) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(resource.listChildren(),
                Spliterator.ORDERED | Spliterator.IMMUTABLE), false).filter(childSelector);
    }

}
