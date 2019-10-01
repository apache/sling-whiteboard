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

import org.apache.sling.commons.html.HtmlElement;

/**
 * Defines the service that will be used to modify the html as it's passed back
 *
 */
public interface TransformationStep {

    /**
     * Called at the beginning of the filter transformation process before any steps
     * are triggered
     * 
     * @param context
     */
    default void before(TransformationContext context) {
    }

    /**
     * called for each element
     * 
     * @param element
     * @param context
     */
    public void step(HtmlElement element, TransformationContext context);

    /**
     * Called after all steps have occurred but before a flush has been
     * triggered
     * 
     * @param context
     */
    default void after(TransformationContext context) {
    }

}
