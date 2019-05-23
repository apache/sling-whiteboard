package org.apache.sling.uca.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Main {

    public static void main(String[] args) throws MalformedURLException, IOException {
        
        if ( args.length != 1 )
            throw new IllegalArgumentException("Usage: java -jar ... <URL>");

        URLConnection con = new URL(args[0]).openConnection();
        System.out.println("Connection type is " + con);
        
        try (InputStream in = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ( (line = br.readLine()) != null )
                System.out.println("[WEB] " + line);
        }

    }

}
