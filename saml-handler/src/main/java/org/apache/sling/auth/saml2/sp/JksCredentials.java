package org.apache.sling.auth.saml2.sp;

import org.apache.sling.auth.saml2.SAML2RuntimeException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public abstract class JksCredentials {

    public static KeyStore getKeyStore(String filePathToJKS, char[] jksPassword) {
        // Try-with-Resources closes file input stream automatically
        try (InputStream fis = new FileInputStream(filePathToJKS)){
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fis, jksPassword);
            return keyStore;
        } catch (FileNotFoundException e) {
            throw new SAML2RuntimeException(e);
        } catch (IOException e) {
            throw new SAML2RuntimeException(e);
        } catch (java.security.KeyStoreException e) {
            throw new SAML2RuntimeException(e);
        }  catch (NoSuchAlgorithmException e) {
            throw new SAML2RuntimeException(e);
        } catch (CertificateException e) {
            throw new SAML2RuntimeException(e);
        }
    }
}
