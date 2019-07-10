package org.apache.sling.graalvm.sling;

import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;

@Component(service=ServiceUserMapper.class)
public class MockServiceUserMapper implements ServiceUserMapper {
    @Override
    public Iterable<String> getServicePrincipalNames(Bundle arg0, String arg1) {
        return null;
    }

    @Override
    public String getServiceUserID(Bundle arg0, String arg1) {
        return "MOCK-USER_" + getClass().getName();
    }
}