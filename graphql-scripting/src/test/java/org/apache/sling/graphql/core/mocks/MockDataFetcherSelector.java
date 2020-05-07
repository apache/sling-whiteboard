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

package org.apache.sling.graphql.core.mocks;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.graphqljava.DataFetcherProvider;
import org.apache.sling.graphql.core.schema.DataFetcherSelector;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MockDataFetcherSelector extends DataFetcherSelector {

    public MockDataFetcherSelector() {
        super(new EchoDataFetcherFactory(), new StaticDataFetcherFactory(), new DigestDataFetcherFactory());
    }

    static class EchoDataFetcher implements DataFetcher<Object> {

        private final Resource r;

        EchoDataFetcher(Resource r) {
            this.r = r;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) {
            return r;
        }

    }

    static class EchoDataFetcherFactory implements DataFetcherProvider {

        @Override
        public String getNamespace() {
            return "test";
        }

        @Override
        public String getName() {
            return "echo";
        }

        @Override
        public DataFetcher<Object> createDataFetcher(Resource r, String name, String options, String source) {
            return new EchoDataFetcher(r);
        }
    }

    static class StaticDataFetcher implements DataFetcher<Object> {

        private final Object data;

        StaticDataFetcher(Object data) {
            this.data = data;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) {
            return data;
        }

    }

    static class StaticDataFetcherFactory implements DataFetcherProvider {

        @Override
        public String getNamespace() {
            return "test";
        }

        @Override
        public String getName() {
            return "static";
        }

        @Override
        public DataFetcher<Object> createDataFetcher(Resource r, String name, String options, String source) {
            Map<String, Object> data = new LinkedHashMap<>(4);
            data.put("test", true);
            return new StaticDataFetcher(data);
        }
    }

    static class DigestDataFetcher implements DataFetcher<Object> {

        private final Resource r;
        private final String algorithm;
        private final String source;

        DigestDataFetcher(Resource r, String options, String source) {
            this.r = r;
            this.algorithm = options;
            this.source = source;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) {
            String rawValue = null;
            if ("path".equals(source)) {
                rawValue = r.getPath();
            } else if("resourceType".equals(source)) {
                rawValue = r.getResourceType();
            }

            String digest = null;
            try {
                digest = computeDigest(algorithm, rawValue);
            } catch (Exception e) {
                throw new RuntimeException("Error computing digest:" + e, e);
            }

            return algorithm + "#" + source + "#" + digest;
        }

    }

    public static String toHexString(byte[] data) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static String computeDigest(String algorithm, String value) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(value.getBytes("UTF-8"));
        return toHexString(md.digest());
    }

    static class DigestDataFetcherFactory implements DataFetcherProvider {

        @Override
        public String getNamespace() {
            return "test";
        }

        @Override
        public String getName() {
            return "digest";
        }

        @Override
        public DataFetcher<Object> createDataFetcher(Resource r, String name, String options, String source) {
            return new DigestDataFetcher(r, options, source);
        }
    }
}
