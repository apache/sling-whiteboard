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

package org.apache.sling.commons.metrics.internal;


import org.apache.sling.commons.metrics.Gauge;
import org.apache.sling.commons.metrics.Metric;

public class GaugeImpl<T> implements Gauge<T>, Metric {

    com.codahale.metrics.Gauge<T> gauge;

    public GaugeImpl(com.codahale.metrics.Gauge<T> g) {
        this.gauge = g;
    }

    @Override
    public <A> A adaptTo(Class<A> type) {
        if (type == com.codahale.metrics.Gauge.class){
            return (A) gauge;
        }
        return null;
    }

    @Override
    public T getValue() {
        return this.gauge.getValue();
    }

}
