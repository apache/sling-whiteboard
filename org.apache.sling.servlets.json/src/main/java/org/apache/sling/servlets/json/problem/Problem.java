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
package org.apache.sling.servlets.json.problem;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ConsumerType;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@ConsumerType
@JsonInclude(Include.NON_NULL)
public class Problem {

    private final URI type;
    private final String title;
    private final int status;
    private final String detail;
    private final URI instance;

    @JsonAnySetter
    @JsonAnyGetter
    private final Map<String, Object> custom;

    /**
     * @param type
     * @param title
     * @param status
     * @param detail
     * @param instance
     * @param custom
     */
    @JsonCreator
    public Problem(@JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("status") int status,
            @JsonProperty("detail") String detail,
            @JsonProperty("instance") String instance) {
        this.type = Optional.ofNullable(type).map(URI::create).orElse(null);
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = Optional.ofNullable(instance).map(URI::create).orElse(null);
        this.custom = new HashMap<>();
    }

    /**
     * @return the type
     */
    public URI getType() {
        return type;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return the detail
     */
    public String getDetail() {
        return detail;
    }

    /**
     * @return the instance
     */
    public URI getInstance() {
        return instance;
    }

    /**
     * @return the custom
     */
    @JsonIgnore
    public Map<String, Object> getCustom() {
        return custom;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */

    @Override
    public String toString() {
        return "Problem [custom=" + custom + ", detail=" + detail + ", instance=" + instance + ", status=" + status
                + ", title=" + title + ", type=" + type + "]";
    }

}
