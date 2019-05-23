package org.apache.sling.uca.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Main {
    
    public static void main(String[] args) throws MalformedURLException, IOException {
        
        new URL("http://sling.apache.org").openConnection();
    }

}
