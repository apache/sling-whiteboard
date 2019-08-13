package org.apache.sling.transformer;

import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.transformer.impl.Process;

public interface TransformationStep {

    public void handle(HtmlElement element, Process process);

}
