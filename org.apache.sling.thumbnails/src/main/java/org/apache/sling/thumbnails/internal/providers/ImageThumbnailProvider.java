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
package org.apache.sling.thumbnails.internal.providers;

import java.io.InputStream;

import com.google.common.net.MediaType;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.osgi.service.component.annotations.Component;

/**
 * A thumbnail provider for image files.
 */
@Component(service = ThumbnailProvider.class, immediate = true)
public class ImageThumbnailProvider implements ThumbnailProvider {

    @Override
    public boolean applies(Resource resource, String metaType) {
        return MediaType.parse(metaType).is(MediaType.ANY_IMAGE_TYPE)
                && !MediaType.SVG_UTF_8.is(MediaType.parse(metaType));
    }

    @Override
    public InputStream getThumbnail(Resource resource) {
        return resource.adaptTo(InputStream.class);
    }

}
