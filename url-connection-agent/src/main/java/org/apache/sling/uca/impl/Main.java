package org.apache.sling.uca.impl;

import java.util.HashMap;
import java.util.Map;

public class Main {
    
    public static void main(String[] args) {
        
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");
        map.put("foo", "baz");
        
        System.out.println(map.get("foo"));
    }

}
