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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.commons.html.Html;
import org.apache.sling.commons.html.util.HtmlElements;
import org.apache.sling.transformer.TransformationContext;
import org.apache.sling.transformer.TransformationStep;

public class TransformationWriter extends Writer {

    private Writer originalWriter;

    private TransformationStepWrapper wrapper;

    private CharBuffer buffer;

    private List<TransformationStep> steps;

    public TransformationWriter(TransformationContext context) throws IOException {
        super();
        this.originalWriter = context.getWriter();
        this.wrapper = new TransformationStepWrapper(new NonceTransformer(), context);
        this.buffer = ByteBuffer.allocate(1024).asCharBuffer();
        this.steps = context.getSteps();
        
   
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (len + buffer.position() > buffer.limit()) {
            flushLocal();
        }
        if (len < buffer.limit()) {
            buffer.put(cbuf, off, len);
        } else {
            writeLocal(String.valueOf(cbuf, off, len));
        }
    }
    
    private void flushLocal() throws IOException {
        buffer.flip();
        writeLocal(buffer.toString());
        buffer.clear();
    }
    
    private void writeLocal(String string) throws IOException {
        String cache = Html.stream(string).flatMap(wrapper).map(HtmlElements.TO_HTML).collect(Collectors.joining());
        originalWriter.write(cache.toCharArray(), 0, cache.toCharArray().length);
    }

    @Override
    public void flush() throws IOException {
        flushLocal();
        originalWriter.flush();
    }

    @Override
    public void close() throws IOException {
        originalWriter.close();
    }

}
