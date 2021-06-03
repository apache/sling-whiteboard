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
package org.apache.sling.sitemap.impl.builder;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * This class implements a {@link XMLStreamWriter} calling a delegate {@link XMLStreamWriter}. It is restricted to
 * writer operations only within a given namespace. Also prefixes are ignored and the declared one for the given
 * namespace from the root scope is used. Any other methods throw {@link UnsupportedOperationException}.
 */
class ExtensionWriter implements XMLStreamWriter {

    private final XMLStreamWriter delegate;
    private final String namespace;

    ExtensionWriter(XMLStreamWriter delegate, String namespace) {
        this.delegate = delegate;
        this.namespace = namespace;
    }

    private void ensureCurrentNamespace(String givenNamespace) {
        if (!namespace.equals(givenNamespace)) {
            throw new IllegalArgumentException("Writing with another namespace not permitted: " + givenNamespace);
        }
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        writeStartElement(namespace, localName);
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        ensureCurrentNamespace(namespaceURI);
        delegate.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        ensureCurrentNamespace(namespaceURI);
        delegate.writeStartElement(localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        delegate.writeEmptyElement(namespace, localName);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        ensureCurrentNamespace(namespaceURI);
        delegate.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        ensureCurrentNamespace(namespaceURI);
        delegate.writeEmptyElement(localName, namespaceURI);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        delegate.writeEndElement();
    }

    @Override
    public void writeEndDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        delegate.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        ensureCurrentNamespace(namespaceURI);
        delegate.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDefaultNamespace(String namespaceURI) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeComment(String data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeProcessingInstruction(String target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeProcessingInstruction(String target, String data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        delegate.writeCData(data);
    }

    @Override
    public void writeDTD(String dtd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeEntityRef(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeStartDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeStartDocument(String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeStartDocument(String encoding, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        delegate.writeCharacters(text);
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        delegate.writeCharacters(text, start, len);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultNamespace(String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        return delegate.getProperty(name);
    }
}
