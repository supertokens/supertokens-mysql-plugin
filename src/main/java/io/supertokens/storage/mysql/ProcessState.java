package io.supertokens.storage.mysql;

import java.util.ArrayList;
import java.util.List;

public class ProcessState extends ResourceDistributor.SingletonResource {

    private static final String RESOURCE_KEY = "io.supertokens.storage.mysql.ProcessState";
    private final List<EventAndException> history = new ArrayList<>();

    private ProcessState() {

    }

    public static ProcessState getInstance(Start main) {
        ResourceDistributor.SingletonResource instance = main.getResourceDistributor().getResource(RESOURCE_KEY);
        if (instance == null) {
            instance = main.getResourceDistributor().setResource(RESOURCE_KEY, new ProcessState());
        }
        return (ProcessState) instance;
    }

    public synchronized EventAndException getLastEventByName(PROCESS_STATE processState) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).state == processState) {
                return history.get(i);
            }
        }
        return null;
    }

    public synchronized void addState(PROCESS_STATE processState, Exception e) {
        if (Start.isTesting) {
            history.add(new EventAndException(processState, e));
        }
    }

    /**
     * CREATING_NEW_TABLE: When the program is attempting to create new tables.
     * DEADLOCK_FOUND: In case of a deadlock situation, we put this event
     */
    public enum PROCESS_STATE {
        CREATING_NEW_TABLE, DEADLOCK_FOUND
    }

    public static class EventAndException {
        public Exception exception;
        PROCESS_STATE state;

        public EventAndException(PROCESS_STATE state, Exception e) {
            this.state = state;
            this.exception = e;
        }
    }

}
