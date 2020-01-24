package io.supertokens.storage.mysql;

import java.util.HashMap;
import java.util.Map;

public class ConnectionPoolTestContent extends ResourceDistributor.SingletonResource {

    public static final String TIME_TO_WAIT_TO_INIT = "timeToWaitToInit";
    public static final String RETRY_INTERVAL_IF_INIT_FAILS = "retryIntervalIfInitFails";
    private static final String RESOURCE_ID = "io.supertokens.storage.mysql.ConnectionPoolTestContent";
    private Map<String, Object> keyValue = new HashMap<String, Object>();

    private ConnectionPoolTestContent() {

    }

    public static ConnectionPoolTestContent getInstance(Start start) {
        ResourceDistributor.SingletonResource resource = start.getResourceDistributor().getResource(RESOURCE_ID);
        if (resource == null) {
            resource = start.getResourceDistributor().setResource(RESOURCE_ID, new ConnectionPoolTestContent());
        }
        return (ConnectionPoolTestContent) resource;
    }

    public void setKeyValue(String key, Object value) {
        this.keyValue.put(key, value);
    }

    @SuppressWarnings("unchecked")
    <T> T getValue(String key) {
        return (T) this.keyValue.get(key);
    }

}
