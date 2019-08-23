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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.commons.html.HtmlElementType;
import org.apache.sling.commons.html.util.ElementFactory;
import org.apache.sling.transformer.TransformationContext;
import org.apache.sling.transformer.TransformationStep;
import org.osgi.service.component.annotations.Component;

@Component(property = { "extension=html", "path=/content/*", "type=REQUEST" })
public class LinkTransformer implements TransformationStep {

    private enum State {
        IN, OUT
    }

    public void handle(HtmlElement element, TransformationContext process) {

        Map<String, Object> context = process.getStateMap();
        State current = (State) context.getOrDefault("currentState", State.OUT);

        MessageDigest d = (MessageDigest) context.computeIfAbsent("hash", (value) -> {
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return digest;
        });

        switch (current) {
        case IN:
            switch (element.getType()) {
            case END_TAG:
                context.put("currentState", State.OUT);
                break;
            case TEXT:
                d.update(element.getValue().getBytes());
                break;
            default:
                break;
            }
            break;
        case OUT:
            switch (element.getType()) {
            case START_TAG:
                String tag = element.getValue();
                if (tag.equals("script") || tag.equals("style")) {
                    context.put("currentState", State.IN);
                }
                break;
            case END_TAG:
                if (element.getValue().equalsIgnoreCase("body")) {
                    String headerValue = Base64.getEncoder().encodeToString(d.digest());
                    process.getResponse().setHeader("Sucks", headerValue);
                    HtmlElement br = ElementFactory.create(HtmlElementType.START_TAG, "br");
                    br.setAttribute("data-hash",headerValue );
                    process.next(br);
                }
                break;
            default:
                break;
            }
            break;
        default:
            break;

        }

        process.next(element);
    }

}
