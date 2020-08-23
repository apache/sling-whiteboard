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
package org.apache.sling.transformer.impl;

import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.transformer.TransformationContext;
import org.apache.sling.transformer.TransformationStep;

/**
 * Class that is used to convert a TransformationStep class into a Function object 
 * that will be used to Map an HtmlElement to a Stream<HtmlElement>
 * 
 */
public class TransformationStepWrapper implements Function<HtmlElement, Stream<HtmlElement>> {

    private TransformationStep tStep;
    private TransformationContext transformationContext;

    public TransformationStepWrapper(TransformationStep step,TransformationContext context) {
        this.tStep = step;
        this.transformationContext = context;
    }

    @Override
    public Stream<HtmlElement> apply(HtmlElement element) {
        tStep.step(element, transformationContext);
        return transformationContext.getElements();
    }
    
}
