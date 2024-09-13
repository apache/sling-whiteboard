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
package org.apache.sling.mdresource.impl.md.handler;

import org.apache.sling.mdresource.impl.md.ProcessingResult;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.util.ast.Node;

public class HeadingHandler implements NodeHandler {

    private boolean hasTitle = false;

    @Override
    public boolean consume(final Node n, final ProcessingResult result) {
        if ( !hasTitle && n instanceof Heading ) {
            final Heading h = (Heading) n;
            if ( h.getLevel() == 1 ) {
                if (result.title == null) {
                    result.title = h.getText().toString();
                }
                this.hasTitle = true;
            }
        }
        return false;
    }
}

