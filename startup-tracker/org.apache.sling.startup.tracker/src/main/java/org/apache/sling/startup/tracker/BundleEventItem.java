package org.apache.sling.startup.tracker;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.BundleEvent;

public class BundleEventItem extends AbstractStartupItem {

    public static final Map<Integer, String> EVENT_TYPES = Stream
            .of(new Object[][] { { 0x00000001, "INSTALLED" }, { 0x00000002, "STARTED" }, { 0x00000004, "STOPPED" },
                    { 0x00000008, "UPDATED" }, { 0x00000010, "UNINSTALLED" }, { 0x00000020, "RESOLVED" },
                    { 0x00000040, "UNRESOLVED" }, { 0x00000080, "STARTING" }, { 0x00000100, "STOPPING" },
                    { 0x00000200, "LAZY_ACTIVATION" }, })
            .collect(Collectors.collectingAndThen(Collectors.toMap(data -> (Integer) data[0], data -> (String) data[1]),
                    Collections::<Integer, String>unmodifiableMap));

    private final String event;
    private final String bundlePid;

    public BundleEventItem(BundleEvent event) {
        super();
        this.setComplete(System.currentTimeMillis());

        this.bundlePid = event.getBundle().getSymbolicName();
        this.event = EVENT_TYPES.get(event.getType());

    }

    @Override
    public String getId() {
        return ItemType.getId(ItemType.BUNDLE_EVENT, bundlePid + "/" + event);
    }

    @Override
    public String getMessage() {
        return String.format("Bundle %s: %s", bundlePid, event);
    }

    @Override
    public String getParentId() {
        return ItemType.getId(ItemType.BUNDLE, bundlePid);
    }

}
