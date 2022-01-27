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
package org.apache.sling.maven.repoinit;

import java.io.File;

public class ContentFolder {

    public enum Type {
        JSON, VLT_XML
    }

    private File folder;
    private String path;
    private Type type;

    /**
     * 
     */
    public ContentFolder() {
        this.type = Type.VLT_XML;
    }

    /**
     * @param folder
     * @param path
     * @param type
     */
    public ContentFolder(File folder, String path, Type type) {
        this.folder = folder;
        this.path = path;
        this.type = type;
    }

    /**
     * @return the folder
     */
    public File getFolder() {
        return folder;
    }

    /**
     * @param folder the folder to set
     */
    public void setFolder(File folder) {
        this.folder = folder;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ContentFolder [folder=" + folder + ", path=" + path + ", type=" + type + "]";
    }

}
