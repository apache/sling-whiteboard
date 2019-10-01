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
package org.apache.sling.resource.encryption.impl;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resource.encryption.EncryptionProvider;
import org.apache.sling.resource.encryption.wrapper.EncryptableValueMapDecorator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(service = { AdapterFactory.class }, property = {
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        Constants.SERVICE_DESCRIPTION + "=Default SlingScriptResolver",
        org.apache.sling.api.adapter.AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource",
        org.apache.sling.api.adapter.AdapterFactory.ADAPTER_CLASSES
                + "=org.apache.sling.resource.encryption.EncryptableValueMap" })
public class EncryptableValueMapAdapterFactory implements AdapterFactory {

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private EncryptionProvider encryptionProvider;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Object adaptable, Class<T> type) {
        ValueMap map = ((Resource) adaptable).adaptTo(ModifiableValueMap.class);
        if (map == null) {
            map = ((Resource) adaptable).adaptTo(ValueMap.class);
        }
        return (T) new EncryptableValueMapDecorator(map, encryptionProvider);
    }
}
