package org.apache.sling.startup.tracker;

public class BundleStartup extends AbstractStartupItem {

    private final String bundlePid;
    private final String startLevel;

    public BundleStartup(String startLevel, String bundlePid) {
        super();
        this.bundlePid = bundlePid;
        this.startLevel = startLevel;
    }

    @Override
    public String getId() {
        return ItemType.getId(ItemType.BUNDLE, bundlePid);
    }

    @Override
    public String getMessage() {
        return "Bundle " + bundlePid;
    }

    @Override
    public String getParentId() {
        return ItemType.getId(ItemType.START_LEVEL, startLevel);
    }
}
