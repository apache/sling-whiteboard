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
 *
 */

package org.apache.sling.auth.saml2.impl;

import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import static org.junit.Assert.fail;

public class JKSHelper {
    private static Logger logger = LoggerFactory.getLogger(JKSHelper.class);
    public static String KEYSTORE_TEST_PATH = "./target/exampleSaml2.jks";
    public static char[] KEYSTORE_TEST_PASSWORD = "password".toCharArray();
    public static String IDP_ALIAS = "idpcertalias";
    public static String SP_ALIAS = "spalias";
    public static char[] SP_TEST_PASSWORD = "sppassword".toCharArray();
    static String PATH = "";

    public static KeyStore createExampleJks(){
        PATH = KEYSTORE_TEST_PATH;
        return createExampleJks(KEYSTORE_TEST_PATH);
    }

    public static KeyStore createExampleJks(String path){
        PATH = path;
        File file = new File(PATH);
        try (FileOutputStream fos = new FileOutputStream(file)){
            KeyStore ks;
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (!file.exists()) {
                ks.load(null, KEYSTORE_TEST_PASSWORD);
                ks.store(fos, KEYSTORE_TEST_PASSWORD);
                logger.info("Example JKS created");
            }
            else {
                logger.info("Example JKS exists");
            }
            return ks;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            logger.error("Error encountered creating JKS", e);
        }
        return null;
    }

    public static void addTestingCertsToKeystore(KeyStore testKeyStore){
        try (FileOutputStream fos = new FileOutputStream(PATH)){
            testKeyStore.load(null, KEYSTORE_TEST_PASSWORD);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(4096);
            KeyPair keyPairIDP = keyPairGenerator.generateKeyPair();
            KeyPair keyPairSP = keyPairGenerator.generateKeyPair();
            final X509Certificate certIDP = JKSHelper.generate(keyPairIDP, "SHA256withRSA", "localhost", 730);
            final X509Certificate certSP = JKSHelper.generate(keyPairSP, "SHA256withRSA", "localhost", 730);
            testKeyStore.setCertificateEntry(IDP_ALIAS, certIDP);
            testKeyStore.setKeyEntry(IDP_ALIAS+"key", keyPairIDP.getPrivate(), KEYSTORE_TEST_PASSWORD,  new Certificate[]{certIDP});
            testKeyStore.setKeyEntry(SP_ALIAS, keyPairSP.getPrivate(), SP_TEST_PASSWORD,  new Certificate[]{certSP});
            testKeyStore.store(fos, KEYSTORE_TEST_PASSWORD);
        } catch (Exception e){
            fail(e.getMessage());
        }
    }

    static X509Certificate generate(final KeyPair keyPair, final String hashAlgorithm, final String cn, final int days)
            throws OperatorCreationException, CertificateException, CertIOException {
        final Instant now = Instant.now();
        final Date notBefore = Date.from(now);
        final Date notAfter = Date.from(now.plus(Duration.ofDays(days)));
        final ContentSigner contentSigner = new JcaContentSignerBuilder(hashAlgorithm).build(keyPair.getPrivate());
        final X500Name x500Name = new X500Name("CN=" + cn);
        final X509v3CertificateBuilder certificateBuilder =
            new JcaX509v3CertificateBuilder(x500Name,
                BigInteger.valueOf(now.toEpochMilli()),
                notBefore, notAfter, x500Name, keyPair.getPublic())
                .addExtension(Extension.subjectKeyIdentifier, false, createSubjectKeyId(keyPair.getPublic()))
                .addExtension(Extension.authorityKeyIdentifier, false, createAuthorityKeyId(keyPair.getPublic()))
                .addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        return new JcaX509CertificateConverter()
            .setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner));
    }

    private static SubjectKeyIdentifier createSubjectKeyId(final PublicKey publicKey) throws OperatorCreationException {
        final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        final DigestCalculator digCalc = new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
        return new X509ExtensionUtils(digCalc).createSubjectKeyIdentifier(publicKeyInfo);
    }

    private static AuthorityKeyIdentifier createAuthorityKeyId(final PublicKey publicKey) throws OperatorCreationException {
        final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        final DigestCalculator digCalc = new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
        return new X509ExtensionUtils(digCalc).createAuthorityKeyIdentifier(publicKeyInfo);
    }

}
