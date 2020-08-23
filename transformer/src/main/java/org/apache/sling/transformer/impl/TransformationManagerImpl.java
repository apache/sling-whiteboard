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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.transformer.TransformationConstants;
import org.apache.sling.transformer.TransformationManager;
import org.apache.sling.transformer.TransformationStep;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component
public class TransformationManagerImpl implements TransformationManager {

    // comparison in the same manner of a filter, where the highest ranking goes
    // first
    // in a situation where ranking is identical, then the lowest service id goes
    // first
    private Map<Map<String, Object>, TransformationStep> mapping = new TreeMap<>((map1, map2) -> {
        Long value1 = (Long) map1.getOrDefault(Constants.SERVICE_RANKING, (Long) 0L);
        Long value2 = (Long) map2.getOrDefault(Constants.SERVICE_RANKING, (Long) 0L);
        if (value1 - value2 == 0) {
            value1 = (Long) map1.get(Constants.SERVICE_ID);
            value2 = (Long) map2.get(Constants.SERVICE_ID);
            return value2.compareTo(value1);
        }
        return value1.compareTo(value2);
    });

    @Override
    public List<TransformationStep> getSteps(SlingHttpServletRequest request) {
        List<TransformationStep> steps = new ArrayList<>();
        mapping.forEach((properties, step) -> {
            if (doStep(properties, request)) {
                steps.add(step);
            }
        });
        return steps;
    }

    private boolean doStep(Map<String, Object> properties, SlingHttpServletRequest request) {
        Iterator<String> it = properties.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            switch (key) {
            case TransformationConstants.PATHS:
                String value = (String) properties.get(key);
                if (!request.getRequestURI().matches(value)) {
                    return false;
                }
                break;
            case TransformationConstants.EXTENSIONS:
                String extension = (String) properties.get(key);
                if (!request.getRequestURI().endsWith(extension)) {
                    return false;
                }
                break;
            default:
            }

        }
        return true;
    }

    @Reference(service = TransformationStep.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    void bindTransformationStep(Map<String, Object> properties, TransformationStep step) {
        mapping.put(properties, step);
    }

    void updateTransformationStep(Map<String, Object> properties, TransformationStep step) {
        mapping.put(properties, step);
    }

    void unbindTransformationStep(Map<String, Object> properties) {
        mapping.remove(properties);
    }

}
