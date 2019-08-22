package org.apache.sling.transformer;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;

public interface ProcessManager {

    List<TransformationStep> getSteps(SlingHttpServletRequest request);

}