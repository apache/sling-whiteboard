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
package org.apache.sling.graphql.schema.aggregator.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CapitalizeTest {
    @Test
    public void normalStrings() throws Exception {
        assertEquals("Voici", DefaultSchemaAggregator.capitalize("voici"));
        assertEquals("Ou bien", DefaultSchemaAggregator.capitalize("OU BIEN"));
   }

   @Test
   public void emptyStrings() throws Exception {
       assertEquals("", DefaultSchemaAggregator.capitalize(""));
       assertEquals(null, DefaultSchemaAggregator.capitalize(null));
  }

  @Test
  public void shortStrings() throws Exception {
      assertEquals("A", DefaultSchemaAggregator.capitalize("a"));
      assertEquals("B", DefaultSchemaAggregator.capitalize("B"));
 }
}