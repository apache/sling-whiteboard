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
package org.apache.sling.transformer;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.html.HtmlElement;

/**
 * The context for a processor invocation.
 */
public interface TransformationContext {

    /***
     * Original Request Object
     * 
     * @return request
     */
    SlingHttpServletRequest getRequest();

    /**
     * Original Response Object
     * 
     * @return response
     */
    SlingHttpServletResponse getResponse();

    /**
     * A String based state map to pass data between subsequent calls to a
     * transformation step
     * 
     * @return state map
     */
    Map<String, Object> getState();

    /**
     * Html elements that will be processed by the next Transformation Step
     * 
     * @param elements
     */
    void doNextStep(HtmlElement... elements);

    /**
     * Html Elements that were identified as being processed in the next step
     * 
     * @return elements for next step
     */
    Stream<HtmlElement> getElements();

    /**
     * List of transformation steps
     * 
     * @return steps
     */
    List<TransformationStep> getSteps();
}
