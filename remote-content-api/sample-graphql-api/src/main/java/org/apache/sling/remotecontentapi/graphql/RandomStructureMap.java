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
 */

 package org.apache.sling.remotecontentapi.graphql;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Return a nested Map of random data, for testing unpredictable structures */
class RandomStructureMap {

    private static final Random random = new Random(42);

    private RandomStructureMap() {
    }
    
    private static Object[] randomArray() {
        final Object[] result = new Object[random.nextInt(3) + 1];
        for(int i=0; i < result.length; i++) {
            result[i] = randomValue();
        }
        return result;
    }

    private static Object randomValue() {
        switch(random.nextInt(4)) {
            case 0: return "It is now " + new Date();
            case 1: return random.nextInt(2) > 0;
            case 2: return randomArray();
            default: return random.nextInt(451);
        }
    }

    private static Map<String, Object> randomMap(int maxEntries) {
        int counter=1;
        final Map<String, Object> result = new HashMap<>();
        while(maxEntries > 0) {
            result.put("key" + counter++, randomValue());
            if(random.nextInt(2) == 1) {
                final int maxSub = random.nextInt(maxEntries) / (random.nextInt(2) + 1);
                result.put("sub" + counter++, randomMap(maxSub));
                maxEntries -= maxSub;
            }
            maxEntries--;
        }
        return result;
    }

    public static Map<String, Object> get() {
        Map<String, Object> result = randomMap(random.nextInt(24));
        result.put(
            "info", 
            "The contents of this map are random, to demonstrate unpredictable content structures"
        );
        return result;
    }
}