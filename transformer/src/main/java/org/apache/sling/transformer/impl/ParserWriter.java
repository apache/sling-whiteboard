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

import org.apache.sling.commons.html.Html;
import org.apache.sling.commons.html.util.HtmlElements;

public class ParserWriter extends Writer {

    private Writer originalWriter;

    private TransformationStepWrapper wrapper;

    public ParserWriter(TransformationContextImpl process) throws IOException {
        this.originalWriter = process.getWriter();
        this.wrapper = new TransformationStepWrapper(new LinkTransformer(), process);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        String foo = Html.stream(String.valueOf(cbuf, off, len))
                .flatMap(wrapper)
                .map(HtmlElements.TO_HTML)
                .collect(Collectors.joining());
        originalWriter.write(foo.toCharArray(), 0, foo.toCharArray().length);
    }

    @Override
    public void flush() throws IOException {
        originalWriter.flush();
    }

    @Override
    public void close() throws IOException {
        originalWriter.close();
    }

}
