package org.apache.sling.startup.tracker;

public abstract class AbstractStartupItem {
    private final long start;
    private long complete;

    protected AbstractStartupItem() {
        this.start = System.currentTimeMillis();
    }

    public long getComplete() {
        return complete;
    }

    public void setComplete(long complete) {
        this.complete = complete;
    }

    public abstract String getId();

    public abstract String getMessage();

    public abstract String getParentId();

    public long getStart() {
        return start;
    }

}
