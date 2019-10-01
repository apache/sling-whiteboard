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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;
import java.util.HashMap;
import java.util.zip.CRC32;

import javax.crypto.spec.SecretKeySpec;

import org.apache.sling.resource.encryption.KeyProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@Component(immediate = true, property = { Constants.SERVICE_DESCRIPTION + "=Sling Java Key Store Provisioner",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        KeyProvider.TYPE + "=OSGi" }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = OSGiKeyProvider.Configuration.class)
public class OSGiKeyProvider implements KeyProvider {

    @ObjectClassDefinition(name = "Apache Sling Encryption Key Provider - OSGi", description = "Apache Sling KeyProvider Implementation - OSGi")
    public @interface Configuration {

        @AttributeDefinition(name = "Key Algorithm", description = "Algorithm used to generate the key", options = {
                @Option(label = "AES", value = "AES") })
        String keyAlgorithm() default "AES";

        @AttributeDefinition(name = "Primary", description = "Base64 encoded primary key", type = AttributeType.PASSWORD)
        String primaryAlias();

        @AttributeDefinition(name = "Secondary", required = false, description = "Base 64 encoded secondary keys", cardinality = Integer.MAX_VALUE)
        String[] secondaryAliases() default {};

    }

    private Configuration config;

    private HashMap<Long, String> aliasIds;

    private Long primaryId;

    @Activate
    public void init(Configuration config) throws GeneralSecurityException {
        this.config = config;
        // init keystore

        aliasIds = new HashMap<>();
        CRC32 crc = new CRC32();
        String[] secondaryAlias = config.secondaryAliases();
        if (secondaryAlias == null) {
            secondaryAlias = new String[] {};
        }
        for (String alias : secondaryAlias) {
            crc.update(alias.getBytes(StandardCharsets.UTF_8));
            Object prior = aliasIds.put(crc.getValue() & 0xffff, alias);
            if (prior != null) {
                throw new GeneralSecurityException(
                        "Two keys are being used that generate the same CRC-32 hash, please correct");
            }
            crc.reset();
        }

        crc.update(config.primaryAlias().getBytes(StandardCharsets.UTF_8));
        primaryId = crc.getValue() & 0xffff;
        if (aliasIds.containsKey(primaryId)) {
            throw new GeneralSecurityException(String.format(
                    "The primary key %s is either the same as or has the same CRC-32 hash as %s in the secondary keys, please correct",
                    config.primaryAlias(), aliasIds.get(primaryId)));
        }

        aliasIds.put(primaryId, config.primaryAlias());
        crc.reset();
    }

    private byte[] toBytes(long value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
    }

    @Override
    public int getIdLength() {
        return 4;
    }

    @Override
    public byte[] getPrimaryKeyID() {
        return toBytes(primaryId);
    }

    @Override
    public Key getKey(byte[] id) throws GeneralSecurityException {
        Long value = toLong(id);
        String alias = aliasIds.get(value);
        if (alias == null) {
            if (!primaryId.equals(value)) {
                throw new GeneralSecurityException(
                        "ID that was provided does not match to any of the currently maintained aliases");
            }
            alias = config.primaryAlias();
        }
        return new SecretKeySpec(Base64.getDecoder().decode(alias.getBytes()), config.keyAlgorithm());
    }

    private Long toLong(byte[] bytes) {
        long reply = 0;
        for (byte b : bytes) {
            reply = reply << 8;
            reply = reply | (b & 0xff);
        }
        return reply;
    }

}
