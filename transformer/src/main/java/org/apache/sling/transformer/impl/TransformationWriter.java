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

import java.io.IOException;
import java.io.Writer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.commons.html.Html;
import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.commons.html.util.HtmlElements;
import org.apache.sling.transformer.TransformationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformationWriter extends Writer {

    private Writer originalWriter;

    private StringBuilder buffer;

    private TransformationStepWrapper[] steps;
    
    private static final Logger log = LoggerFactory.getLogger(TransformationWriter.class);

    public TransformationWriter(TransformationContext context) throws IOException {
        super();
        log.debug("TransformationWriter initialized with {} steps", context.getState());
        this.originalWriter = context.getResponse().getWriter();
        this.buffer = new StringBuilder();
        steps = context.getSteps().stream().map( step -> {
            return new TransformationStepWrapper(step, context);
        }).toArray(TransformationStepWrapper[]::new);
        
   
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        buffer.append(String.copyValueOf(cbuf, off, len));
    }
    
    private void writeLocal(String string) throws IOException {
        Stream<HtmlElement> stream = Html.stream(string);
        for (int index = 0; index < steps.length; ++index ) {
            stream = stream.flatMap(steps[index]);
            log.error("adding {}", steps[index]);
        }
        char[] cache = stream.map(HtmlElements.TO_HTML).collect(Collectors.joining()).toCharArray();
        originalWriter.write(cache, 0, cache.length);
    }

    @Override
    public void flush() throws IOException {
        writeLocal(buffer.toString());
        buffer.setLength(0);
        originalWriter.flush();
    }

    @Override
    public void close() throws IOException {
        originalWriter.close();
    }

}
