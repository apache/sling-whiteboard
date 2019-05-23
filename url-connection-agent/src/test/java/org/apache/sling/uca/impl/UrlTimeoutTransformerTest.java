package org.apache.sling.uca.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import javassist.NotFoundException;

public class UrlTimeoutTransformerTest {
    
    private URLTimeoutTransformer transformer;

    @Before
    public void initFields() {
        transformer = new URLTimeoutTransformer(1, 1);
    }

    @Test
    public void findDeclaredConnectMethod() throws NotFoundException {
        assertNotNull(transformer.findConnectMethod("sun/net/www/protocol/http/HttpURLConnection"));
    }

    @Test(expected = NotFoundException.class)
    public void findInheritedConnectMethod() throws NotFoundException {
        // do NOT look for inherited methods, as we can only rewrite the precise classes the
        // retransform was triggered for
        transformer.findConnectMethod("sun/net/www/protocol/https/DelegateHttpsURLConnection");
    }
    
}
