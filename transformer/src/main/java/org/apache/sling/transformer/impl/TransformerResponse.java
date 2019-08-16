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
import java.io.Writer;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

class TransformerResponse
    extends SlingHttpServletResponseWrapper {

    /** The current request. */
    private final SlingHttpServletRequest request;


    /** wrapped rewriter/servlet writer */
    private PrintWriter writer;

    /** response content type */
    private String contentType;

    /**
     * Initializes a new instance.
     * @param request The sling request.
     * @param delegatee The SlingHttpServletResponse wrapped by this instance.
     */
    public TransformerResponse(SlingHttpServletRequest request,
                            SlingHttpServletResponse delegatee) {
        super(delegatee);
        this.request = request;
    }

    /**
     * @see javax.servlet.ServletResponseWrapper#setContentType(java.lang.String)
     */
    public void setContentType(String type) {
        this.contentType = type;
        super.setContentType(type);
    }

    /**
     * Wraps the underlying writer by a rewriter pipeline.
     *
     * @see javax.servlet.ServletResponseWrapper#getWriter()
     */
    public PrintWriter getWriter() throws IOException {
        if ( this.writer == null ) {
            this.writer = super.getWriter();
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

    /**
     * Inform this response that the request processing is finished.
     * @throws IOException
     */
    public void finished(final boolean errorOccured) throws IOException {

    }

}
