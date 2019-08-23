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
import java.io.PrintWriter;

import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

class TransformerResponse
    extends SlingHttpServletResponseWrapper {

    /** wrapped rewriter/servlet writer */
    private PrintWriter writer;


    private TransformationContextImpl process;


    public TransformerResponse(TransformationContextImpl process) {
        super(process.getResponse());
        this.process = process;
    }

    /**
     * Wraps the underlying writer by a rewriter pipeline.
     *
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        if ( this.writer == null ) {
            this.writer = new PrintWriter(new ParserWriter(process));
        }
        return writer;
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#flushBuffer()
     */
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else {
            super.flushBuffer();
        }
    }


}
