package org.apache.sling.transformer.impl;

import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.commons.html.HtmlElementType;
import org.apache.sling.transformer.TransformationStep;
import org.osgi.service.component.annotations.Component;

@Component(property = {
        "extension=html",
        "path=/content/*",
        "type=REQUEST"
})
public class LinkTransformer implements TransformationStep {

    public void handle(HtmlElement element, Process process) {
        if (element.getType() != HtmlElementType.START_TAG) {
            process.next(element);
            return;
        }
        if (element.containsAttribute("href")) {
            String value = element.getAttributeValue("href");
            if (value != null && value.startsWith("/")) {
                element.setAttribute("href", "http://www.apache.org" + value);
            }
        }
        if (element.containsAttribute("src")) {
            String value = element.getAttributeValue("src");
            if (value != null && value.startsWith("/")) {
                element.setAttribute("src", "http://www.apache.org" + value);
            }
        }
        process.next(element);
    }

}
