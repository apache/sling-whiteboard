package org.apache.sling.startup.tracker;

public enum ItemType {

    BUNDLE, BUNDLE_EVENT, SERVICE, START_LEVEL;

    public static final String getId(ItemType type, String itemId) {
        return type.name() + "::" + itemId;
    }
}
