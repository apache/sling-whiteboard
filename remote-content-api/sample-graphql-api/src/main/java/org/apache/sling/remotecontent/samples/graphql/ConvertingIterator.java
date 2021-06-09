/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.remotecontent.samples.graphql;

import java.util.Iterator;
import java.util.function.Function;

public class ConvertingIterator<S,T> implements Iterator<T> {

    private final Iterator<S> sourceIterator;
    private final Function<S,T> converter;

    ConvertingIterator(Iterator<S> sourceIterator, Function<S,T> converter) {
        this.sourceIterator = sourceIterator;
        this.converter = converter;
    }

    @Override
    public boolean hasNext() {
        return sourceIterator.hasNext();
    }

    @Override
    public T next() {
        return converter.apply(sourceIterator.next());
    }

}