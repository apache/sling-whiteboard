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
package org.apache.sling.jcr.wrappers;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

public class PropertyWrapper extends ItemWrapper<Property> implements Property {
    public PropertyWrapper(SessionWrapper sessionWrapper, Property delegate) {
        super(sessionWrapper, delegate);
    }

    @Override
    public void setValue(Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(values);
    }

    @Override
    public void setValue(String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(values);
    }

    @Override
    public void setValue(InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public void setValue(Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        delegate.setValue(value);
    }

    @Override
    public Value getValue() throws ValueFormatException, RepositoryException {
        return delegate.getValue();
    }

    @Override
    public Value[] getValues() throws ValueFormatException, RepositoryException {
        return delegate.getValues();
    }

    @Override
    public String getString() throws ValueFormatException, RepositoryException {
        return delegate.getString();
    }

    @Override
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return delegate.getStream();
    }

    @Override
    public Binary getBinary() throws ValueFormatException, RepositoryException {
        return delegate.getBinary();
    }

    @Override
    public long getLong() throws ValueFormatException, RepositoryException {
        return delegate.getLong();
    }

    @Override
    public double getDouble() throws ValueFormatException, RepositoryException {
        return delegate.getDouble();
    }

    @Override
    public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
        return delegate.getDecimal();
    }

    @Override
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return delegate.getDate();
    }

    @Override
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return delegate.getBoolean();
    }

    @Override
    public Node getNode() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        return this.sessionWrapper.getNode(delegate.getNode().getPath());
    }

    @Override
    public Property getProperty() throws ItemNotFoundException, ValueFormatException, RepositoryException {
        return this.sessionWrapper.getProperty(delegate.getProperty().getPath());
    }

    @Override
    public long getLength() throws ValueFormatException, RepositoryException {
        return delegate.getLength();
    }

    @Override
    public long[] getLengths() throws ValueFormatException, RepositoryException {
        return delegate.getLengths();
    }

    @Override
    public PropertyDefinition getDefinition() throws RepositoryException {
        return delegate.getDefinition();
    }

    @Override
    public int getType() throws RepositoryException {
        return delegate.getType();
    }

    @Override
    public boolean isMultiple() throws RepositoryException {
        return delegate.isMultiple();
    }
}
