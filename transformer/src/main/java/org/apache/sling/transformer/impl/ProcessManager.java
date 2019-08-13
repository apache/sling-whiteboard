package org.apache.sling.transformer.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ProcessManager {
    
    
    @Reference
    private Process processes;
    
    @Reference
    void bindProcess(Process process, Map<String, ?> properties) {
        
    }
    
    void unbindProcess(Process process) {
        
    }

}
