/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.extension.unpack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;

public class UnpackLauncherExtension implements ExtensionHandler
{
    public static final String UNPACK_MAPPING_KEY = UnpackLauncherExtension.class.getPackage().getName() + ".mapping";

    @Override
    public boolean handle(ExtensionContext extensionContext, Extension extension) throws Exception
    {
        if (extension.getType() == ExtensionType.ARTIFACTS)
        {
            String mapping = extensionContext.getFrameworkProperties().get(UNPACK_MAPPING_KEY);

            if (mapping != null && !mapping.isEmpty())
            {
                return Unpack.fromMapping(mapping).handle(extension, new ArtifactProvider() {
                    @Override
                    public URL provide(ArtifactId artifactId)
                    {
                        try
                        {
                            return extensionContext.getArtifactFile(artifactId);
                        }
                        catch (IOException e)
                        {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
            }
        }
        return false;
    }
}
