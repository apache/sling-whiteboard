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

import java.util.Map;
import java.util.UUID;

import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.commons.html.HtmlElementType;
import org.apache.sling.transformer.TransformationContext;
import org.apache.sling.transformer.TransformationStep;
import org.osgi.service.component.annotations.Component;

@Component(property = { "extension=html", "path=/content/*", "type=REQUEST" })
public class NonceTransformer implements TransformationStep {

    @Override
    public void init(TransformationContext context) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        context.getState().put("nonce", nonce);
        context.getResponse().setHeader("X-nonce", nonce);
    }

    public void handle(HtmlElement element, TransformationContext context) {

        Map<String, Object> state = context.getState();
        String nonce = (String)state.get("nonce");

        if (element.getType()== HtmlElementType.START_TAG) {
            if (element.getValue().equals("script")) {
                element.setAttribute("nonce", nonce);
            }
        }
        
        context.doNext(element);
    }

}
