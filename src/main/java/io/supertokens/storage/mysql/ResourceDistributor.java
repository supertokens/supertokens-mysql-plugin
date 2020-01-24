package io.supertokens.storage.mysql;

import java.util.HashMap;
import java.util.Map;

// the purpose of this class is to tie singleton classes to s specific main instance. So that 
// when the main instance dies, those singleton classes die too.

public class ResourceDistributor {

    private final Object lock = new Object();
    private Map<String, SingletonResource> resources = new HashMap<String, SingletonResource>();

    public SingletonResource getResource(String key) {
        return resources.get(key);
    }

    public SingletonResource setResource(String key, SingletonResource resource) {
        synchronized (lock) {
            resources.putIfAbsent(key, resource);
            return resource;
        }
    }

    public static class SingletonResource {

    }

}
