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
package org.apache.sling.cp2fm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.PackageManagerImpl;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONWriter;

public final class ContentPackage2FeatureModelConverter {

    private static final String FEATURE_CLASSIFIER = "cp2fm-converted-feature";

    private static final String SLING_OSGI_FEATURE_TILE_TYPE = "slingosgifeature";

    private static final String JSON_FILE_EXTENSION = ".json";

    private final PackageManager packageManager = new PackageManagerImpl();

    private boolean strictValidation = false;

    public ContentPackage2FeatureModelConverter setStrictValidation(boolean strictValidation) {
        this.strictValidation = strictValidation;
        return this;
    }

    public void convert(File contentPackage, File outputDirectory) throws IOException {
        if (contentPackage == null) {
            throw new IllegalArgumentException("Null content-package can not be converted.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Null output directory not supported, it must be specified.");
        }

        if (!contentPackage.exists() || !contentPackage.isFile()) {
            throw new IllegalArgumentException("Content-package "
                                               + contentPackage
                                               + " does not exist or it is not a valid file.");
        }

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("output directory "
                                            + outputDirectory
                                            + " does not exist and can not be created, please make sure current user '"
                                            + System.getProperty("")
                                            + " has enough rights to write on the File System.");
        }

        VaultPackage vaultPackage = null;
        Feature targetFeature = null;

        try {
            vaultPackage = packageManager.open(contentPackage, strictValidation);

            PackageId packageId = vaultPackage.getId();
            targetFeature = new Feature(new ArtifactId(packageId.getGroup().replace('/', '.'), 
                                                       packageId.getName(),
                                                       packageId.getVersionString(),
                                                       FEATURE_CLASSIFIER,
                                                       SLING_OSGI_FEATURE_TILE_TYPE));

            File targetFile = new File(outputDirectory, packageId.getName() + JSON_FILE_EXTENSION);
            try (FileWriter targetWriter = new FileWriter(targetFile)) {
                FeatureJSONWriter.write(targetWriter, targetFeature);
            }
        } finally {
            if (vaultPackage != null && !vaultPackage.isClosed()) {
                vaultPackage.close();
            }
        }
    }

}
