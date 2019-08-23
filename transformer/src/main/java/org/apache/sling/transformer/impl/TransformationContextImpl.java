/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.transformer.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.html.Html;
import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.transformer.TransformationContext;
import org.apache.sling.transformer.TransformationStep;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Utility class that allows you to define a flatMap process in the form of a
 * BiConsumer<Element,TagMapping> lambda.
 * 
 * This allows you to use the next() method to collect the elements that will be
 * passed on to the stream method. This can modify the eventual output and
 * assists in use cases where there is a need to add or remove elements
 *
 */
@ProviderType
public class TransformationContextImpl implements TransformationContext {

    private List<HtmlElement> list = new ArrayList<>();
    private Map<String, Object> context = new HashMap<>();
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private boolean reset;
    private List<TransformationStep> steps;

    public TransformationContextImpl(SlingHttpServletRequest request, SlingHttpServletResponse response, List<TransformationStep> steps) {
        this.request = request;
        this.response = response;
        this.steps = steps;
    }

    /**
     * Collects all the elements that are either being passed through or created in
     * the accept method of the consumer so that they may be passed on to the next
     * process.
     */
    public void next(HtmlElement... elements) {
        if (reset) {
            list.clear();
            reset = false;
        }
        Collections.addAll(list, elements);
    }

    public void next(String html) {
        if (reset) {
            list.clear();
            reset = false;
        }
        Collections.addAll(list, Html.stream(html).toArray(HtmlElement[]::new));
    }

    public Stream<HtmlElement> getElements() {
        reset = true;
        return list.stream();
    }

    public Map<String, Object> getStateMap() {
        return context;
    }

    public SlingHttpServletResponse getResponse() {
        return response;
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    @Override
    public PrintWriter getWriter() throws IOException  {
        return response.getWriter();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

}
