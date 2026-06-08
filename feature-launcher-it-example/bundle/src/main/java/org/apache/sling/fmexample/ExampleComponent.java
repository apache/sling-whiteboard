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
package org.apache.sling.fmexample;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trivial immediate component. Its only purpose is to give the example bundle something to do, so the
 * integration test can verify the bundle reaches the ACTIVE state once the launched instance is up.
 */
@Component(immediate = true, service = ExampleComponent.class)
public class ExampleComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleComponent.class);

    @Activate
    public void activate() {
        LOG.info("Apache Sling Feature Launcher IT example bundle activated");
    }
}
