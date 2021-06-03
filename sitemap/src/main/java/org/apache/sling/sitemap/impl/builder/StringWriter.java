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

import org.jetbrains.annotations.NotNull;

import java.io.Writer;

/**
 * This class implements an in-memory {@link Writer} using a {@link StringBuilder} as buffer. It can be reused by
 * calling {@link StringWriter#reset()}.
 */
class StringWriter extends Writer {

    private final StringBuilder buf;

    StringWriter() {
        this.buf = new StringBuilder();
    }

    @Override
    public void write(int c) {
        buf.append((char) c);
    }

    @Override
    public void write(char @NotNull [] cbuf) {
        buf.append(cbuf);
    }

    @Override
    public void write(@NotNull String str) {
        buf.append(str);
    }

    @Override
    public void write(@NotNull String str, int off, int len) {
        buf.append(str, off, off + len);
    }

    @Override
    public StringWriter append(CharSequence csq) {
        buf.append(csq);
        return this;
    }

    @Override
    public StringWriter append(CharSequence csq, int start, int end) {
        buf.append(csq, start, end);
        return this;
    }

    @Override
    public StringWriter append(char c) {
        buf.append(c);
        return this;
    }

    @Override
    public void write(char @NotNull [] cbuf, int off, int len) {
        buf.append(cbuf, off, len);
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    public void reset() {
        buf.setLength(0);
    }

    public CharSequence asCharSequence() {
        return buf;
    }
}
