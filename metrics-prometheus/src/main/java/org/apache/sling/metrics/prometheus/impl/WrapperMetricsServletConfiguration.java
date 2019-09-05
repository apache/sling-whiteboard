/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.metrics.prometheus.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Allows to configure the path the metrics exporter servlet will be registered to.
 */
@ObjectClassDefinition(description = "Configuration for the Prometheus Metrics Exporter", name = "Prometheus Metrics Exporter")
@interface WrapperMetricsServletConfiguration {

    @AttributeDefinition(name = "Servlet Path", description = "Path under which the Prometheus Metrics servlet is registered")
    String osgi_http_whiteboard_servlet_pattern() default "/metrics";

}
