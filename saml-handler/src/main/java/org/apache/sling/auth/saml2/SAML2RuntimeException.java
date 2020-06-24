package org.apache.sling.auth.saml2;

/**
 * The SAML2RuntimeException is thrown when problems are encountered when processing SAML2 requests,
 * and it is intended to halt the sign on process.
 *
 *
 * @author cmrockwe@umich.edu
 */
public class SAML2RuntimeException extends RuntimeException {

    public SAML2RuntimeException(String message, Throwable cause){
        super(message, cause);
    }

    public SAML2RuntimeException(Throwable cause){
        super(cause);
    }

    public SAML2RuntimeException(String message){
        super(message);
    }
}
