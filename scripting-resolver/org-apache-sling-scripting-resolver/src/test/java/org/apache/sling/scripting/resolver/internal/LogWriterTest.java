/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver.internal;

import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LogWriterTest {

    @Test
    public void testLogWriterCRLFFlush() {
        Logger mockedLogger = mock(Logger.class);
        LogWriter writer = new LogWriter(mockedLogger);
        char[] buffer = "Test CRLF 1\nTest CRLF 2".toCharArray();
        writer.write(buffer, 0, buffer.length);
        verify(mockedLogger).error("Test CRLF 1");
        writer.flush();
        verify(mockedLogger).error("Test CRLF 2");
        verifyNoMoreInteractions(mockedLogger);
        writer.flush();
        writer.close();
    }

    @Test
    public void testLogWriterCharWriteAndFlush() {
        Logger mockedLogger = mock(Logger.class);
        LogWriter writer = new LogWriter(mockedLogger);
        writer.write('a');
        writer.write('\n');
        verify(mockedLogger).error("a");
        writer.write('a');
        writer.write('b');
        writer.write('c');
        writer.close();
        verify(mockedLogger).error("abc");
    }



}
