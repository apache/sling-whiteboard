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
package org.apache.sling.sitemap.builder.extensions;

import org.apache.sling.sitemap.builder.Extension;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Locale;

/**
 * An extension to add alternate language links to an {@link org.apache.sling.sitemap.builder.Url}.
 *
 * @see <a href="https://developers.google.com/search/docs/advanced/crawling/localized-versions#sitemap">Tell Google about localized versions of your page</a>
 */
@ProviderType
public interface AlternateLanguageExtension extends Extension {

    @NotNull
    AlternateLanguageExtension setLocale(@NotNull Locale locale);

    @NotNull
    AlternateLanguageExtension setDefaultLocale();

    @NotNull
    AlternateLanguageExtension setHref(@NotNull String location);

}
