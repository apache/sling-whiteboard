package org.apache.sling.uca.impl;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.TimeUnit;

public class Agent {

    public static void premain(String args, Instrumentation inst) {

        System.out.println("[AGENT] Loading agent...");
        String[] parsedArgs = args.split(",");
        long connectTimeout =  TimeUnit.MINUTES.toMillis(1);
        long readTimeout = TimeUnit.MINUTES.toMillis(1);
        if ( parsedArgs.length > 0 )
            connectTimeout = Long.parseLong(parsedArgs[0]);
        if ( parsedArgs.length > 1 )
            readTimeout = Long.parseLong(parsedArgs[1]);
        
        System.out.format("[AGENT] Set connectTimeout : %d, readTimeout: %d%n", connectTimeout, readTimeout);

        URLTimeoutTransformer transformer = new URLTimeoutTransformer(connectTimeout, readTimeout);
        
        inst.addTransformer(transformer, true);
        System.out.println("[AGENT] Loaded agent!");
    }

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }
    
}
