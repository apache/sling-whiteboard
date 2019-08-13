package org.apache.sling.transformer.impl;

import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.sling.commons.html.HtmlElement;
import org.apache.sling.transformer.TransformationStep;

public class TransformationStepWrapper implements Function<HtmlElement, Stream<HtmlElement>> {

    private TransformationStep processStep;
    private Process process;

    public TransformationStepWrapper(TransformationStep processStep,Process process) {
        this.processStep = processStep;
        this.process = process;
    }

    @Override
    public Stream<HtmlElement> apply(HtmlElement element) {
        processStep.handle(element, process);
        return process.getElements();
    }
    
}
