/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.r2f;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.function.Function;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;

final class OSGiConfiguration2FeatureConfigurationMapper
    extends AbstractFeatureElementConsumer<Configuration>
    implements Function<org.osgi.service.cm.Configuration, Configuration> {

    public OSGiConfiguration2FeatureConfigurationMapper(Feature targetFeature) {
        super(targetFeature);
    }

    @Override
    public Configuration apply(org.osgi.service.cm.Configuration sourceConfiguration) {
        Configuration targetConfiguration = new Configuration(sourceConfiguration.getPid());

        Dictionary<String, Object> configurationProperties = sourceConfiguration.getProperties();
        Enumeration<String> keys = configurationProperties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object value = configurationProperties.get(key);

            targetConfiguration.getProperties().put(key, value);
        }

        return targetConfiguration;
    }

    @Override
    public void accept(Configuration configuration) {
        getTargetFeature().getConfigurations().add(configuration);
    }

}
