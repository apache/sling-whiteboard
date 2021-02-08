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
package org.apache.sling.mdresource.impl.md.links;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.renderer.LinkResolverBasicContext;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.util.ast.Node;

public class CustomLinkResolver implements LinkResolver {

    private final Resource baseResource;

    public CustomLinkResolver(final Resource baseResource) {
        this.baseResource = baseResource;
    }


    @Override
    public @NotNull ResolvedLink resolveLink(@NotNull Node node, @NotNull LinkResolverBasicContext context,
            @NotNull ResolvedLink link) {
        if ( link.getLinkType().equals(LinkType.IMAGE) || link.getLinkType().equals(LinkType.LINK) ) {
            String url = link.getUrl();
            if ( url.indexOf(":/") == - 1 && !url.startsWith("/") ) {
                // relative
                final String path = this.baseResource.getPath().concat("/").concat(url);
                link = link.withUrl(this.baseResource.getResourceResolver().map(ResourceUtil.normalize(path)));
            }
        }

        return link;
    }
}
