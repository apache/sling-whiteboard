package org.apache.sling.microsling.api.exceptions;

import javax.servlet.ServletException;

/** Base class for Sling exceptions */
public class SlingException extends ServletException {
    private static final long serialVersionUID = 1L;

    public SlingException(String reason) {
        super(reason);
    }

    public SlingException(String reason,Throwable cause) {
        super(reason,cause);
    }
}
