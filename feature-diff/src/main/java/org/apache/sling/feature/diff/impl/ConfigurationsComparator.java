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
package org.apache.sling.feature.diff.impl;

import static java.util.Objects.deepEquals;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;

import com.google.auto.service.AutoService;

@AutoService(FeatureElementComparator.class)
public final class ConfigurationsComparator extends AbstractFeatureElementComparator {

    public ConfigurationsComparator() {
        super("configurations");
    }

    @Override
    public void computeDiff(Feature previous, Feature current, Feature target) {
        computeDiff(previous.getConfigurations(), current.getConfigurations(), target);
    }

    protected void computeDiff(Configurations previouses, Configurations currents, Feature target) {
        for (Configuration previousConfiguration : previouses) {
            Configuration currentConfiguration = currents.getConfiguration(previousConfiguration.getPid());

            if (currentConfiguration == null) {
                target.getPrototype().getConfigurationRemovals().add(previousConfiguration.getPid());
            } else {
                computeDiff(previousConfiguration, currentConfiguration, target);
            }
        }

        for (Configuration currentConfiguration : currents) {
            Configuration previousConfiguration = previouses.getConfiguration(currentConfiguration.getPid());

            if (previousConfiguration == null) {
                target.getConfigurations().add(currentConfiguration);
            }
        }
    }

    protected void computeDiff(Configuration previous, Configuration current, Feature target) {
        Dictionary<String, Object> previousProperties = previous.getProperties();
        Dictionary<String, Object> currentProperties = current.getProperties();

        Configuration targetConfiguration = new Configuration(previous.getPid());
        Dictionary<String, Object> targetProperties = targetConfiguration.getProperties();

        Enumeration<String> previousKeys = previousProperties.keys();
        while (previousKeys.hasMoreElements()) {
            String previousKey = previousKeys.nextElement();

            // no other way to check if a key was removed in a dictionary
            if (hasKey(previousKey, currentProperties.keys())) {
                Object previousValue = previousProperties.get(previousKey);
                Object currentValue = currentProperties.get(previousKey);

                if (!areEquals(previousValue, currentValue)) {
                    targetProperties.put(previousKey, currentValue);
                }
            }
        }

        Enumeration<String> currentKeys = currentProperties.keys();
        while (currentKeys.hasMoreElements()) {
            String currentKey = currentKeys.nextElement();

            Object previousValue = previousProperties.get(currentKey);
            Object currentValue = currentProperties.get(currentKey);

            if (previousValue == null && currentValue != null) {
                targetProperties.put(currentKey, currentValue);
            }
        }

        if (!targetProperties.isEmpty()) {
            target.getConfigurations().add(targetConfiguration);
        }
    }

    private static boolean areEquals(Object lhs, Object rhs) {
        if (lhs == rhs) {
            return true;
        }

        if (lhs == null ^ rhs == null) {
            return false;
        }

        // Find the leaf class since there may be transients in the leaf
        // class or in classes between the leaf and root.
        // If we are not testing transients or a subclass has no ivars,
        // then a subclass can test equals to a superclass.
        final Class<?> lhsClass = lhs.getClass();
        final Class<?> rhsClass = rhs.getClass();
        Class<?> testClass;

        if (lhsClass.isInstance(rhs)) {
            testClass = lhsClass;
            if (!rhsClass.isInstance(lhs)) {
                // rhsClass is a subclass of lhsClass
                testClass = rhsClass;
            }
        } else if (rhsClass.isInstance(lhs)) {
            testClass = rhsClass;
            if (!lhsClass.isInstance(rhs)) {
                // lhsClass is a subclass of rhsClass
                testClass = lhsClass;
            }
        } else {
            // The two classes are not related.
            return false;
        }

        if (testClass.isArray()) {
            return deepEquals(lhs, rhs);
        } else if (Collection.class.isAssignableFrom(testClass)) {
            return areEquals((Collection<?>) lhs, (Collection<?>) rhs);
        } else if (Map.class.isAssignableFrom(testClass)) {
            return areEquals((Map<?, ?>) lhs, (Map<?, ?>) rhs);
        }

        return Objects.equals(lhs, rhs);
    }

    private static boolean areEquals(Collection<?> lhs, Collection<?> rhs) {
        if (lhs.size() != rhs.size()) {
            return false;
        }

        return deepEquals(lhs.toArray(), rhs.toArray());
    }

    private static boolean areEquals(Map<?, ?> lhs, Map<?, ?> rhs) {
        for (Entry<?, ?> previousEntry : lhs.entrySet()) {
            Object previousKey = previousEntry.getKey();

            if (!rhs.containsKey(previousKey)) {
                return false;
            } else {
                Object previousValue = previousEntry.getValue();
                Object currentValue = rhs.get(previousKey);

                if (!areEquals(previousValue, currentValue)) {
                    return false;
                }
            }
        }

        for (Object currentKey : rhs.keySet()) {
            if (!lhs.containsKey(currentKey)) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasKey(String key, Enumeration<String> keys) {
        while (keys.hasMoreElements()) {
            String current = keys.nextElement();

            if (key.equals(current)) {
                return true;
            }
        }

        return false;
    }

}
