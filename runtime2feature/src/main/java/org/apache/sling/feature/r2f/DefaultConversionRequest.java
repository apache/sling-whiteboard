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

import static java.util.Objects.requireNonNull;

import org.apache.sling.feature.ArtifactId;
import org.osgi.framework.BundleContext;

public class DefaultConversionRequest implements ConversionRequest {

    private ArtifactId resultId;

    private BundleContext bundleContext;

    @Override
    public ArtifactId getResultId() {
        return resultId;
    }

    public DefaultConversionRequest setResultId(String resultId) {
        resultId = requireNonNull(resultId, "Impossible to create the Feature diff with a null id");
        return setResultId(ArtifactId.parse(resultId));
    }

    public DefaultConversionRequest setResultId(ArtifactId resultId) {
        this.resultId = requireNonNull(resultId, "Impossible to create the Feature diff with a null id");
        return this;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public DefaultConversionRequest setBundleContext(BundleContext bundleContext) {
        this.bundleContext = requireNonNull(bundleContext, "Impossible to create the Feature from a null BundleContext");
        return this;
    }

}
