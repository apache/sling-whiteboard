package org.apache.sling.graalvm;

import org.osgi.service.component.annotations.Component;
import java.util.Date;

@Component(service=MsgProvider.class)
public class MsgProviderImpl implements MsgProvider {
    private final String msg;

    MsgProviderImpl(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg + ", at " + new Date();
    }
}