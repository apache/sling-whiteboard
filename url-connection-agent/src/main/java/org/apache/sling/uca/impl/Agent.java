/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.uca.impl;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.TimeUnit;

public class Agent {

    public static void premain(String args, Instrumentation inst) {
        
        String[] parsedArgs = args != null ? args.split(",") : new String[0];
        long connectTimeout =  TimeUnit.MINUTES.toMillis(1);
        long readTimeout = TimeUnit.MINUTES.toMillis(1);
        String logSpec = "";
        if ( parsedArgs.length > 0 )
            connectTimeout = Long.parseLong(parsedArgs[0]);
        if ( parsedArgs.length > 1 )
            readTimeout = Long.parseLong(parsedArgs[1]);
        if ( parsedArgs.length > 2)
            logSpec = parsedArgs[2];
        
        Log.configure(logSpec);
        
        Log.get().log("Preparing to install URL transformers. Configured timeouts - connectTimeout : %d, readTimeout: %d", connectTimeout, readTimeout);

        ClassFileTransformer[] transformers = new ClassFileTransformer[] {
            new JavaNetTimeoutTransformer(connectTimeout, readTimeout),
            new HttpClient3TimeoutTransformer(connectTimeout, readTimeout),
            new HttpClient4TimeoutTransformer(connectTimeout, readTimeout),
            new OkHttpTimeoutTransformer(connectTimeout, readTimeout)
        };
        
        for ( ClassFileTransformer transformer : transformers )
            inst.addTransformer(transformer, true);

        Log.get().log("All transformers installed");
    }    
}
