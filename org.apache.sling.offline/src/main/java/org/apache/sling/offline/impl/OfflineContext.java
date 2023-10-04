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
package org.apache.sling.offline.impl;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(service = {})
public class OfflineContext implements ServletContext{

    @Reference(target="(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT+"=\\(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=org.apache.sling\\))")
    private ServletContextListener listener;

    @Activate
    protected void activate() {
        this.listener.contextInitialized(new ServletContextEvent(this));
    }

    @Deactivate
    protected void deactivate() {
        this.listener.contextDestroyed(null);
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public ServletContext getContext(String uripath) {
        return this;
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return this.getMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion() {
        return this.getMinorVersion();
    }

    @Override
    public String getMimeType(String file) {
        return null;
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        return null;
    }

    @Override
    public String getRealPath(String path) {
        return path;
    }

    @Override
    public String getServerInfo() {
        return "Apache Sling Offline";
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public void setAttribute(String name, Object object) {
    }

    @Override
    public void removeAttribute(String name) {
    }

    @Override
    public String getServletContextName() {
        return "Apache Sling Offline";
    }

    @Override
    public Dynamic addFilter(String filterName, String className) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dynamic addFilter(String filterName, Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(String className) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, String className) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public javax.servlet.ServletRegistration.Dynamic addServlet(String servletName,
            Class<? extends Servlet> servletClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getServletNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVirtualServerName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void log(String msg) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void log(Exception exception, String msg) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void log(String message, Throwable throwable) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // TODO Auto-generated method stub
        
    }

    
}
