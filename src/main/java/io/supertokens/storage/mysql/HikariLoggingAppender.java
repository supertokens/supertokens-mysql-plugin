package io.supertokens.storage.mysql;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import io.supertokens.storage.mysql.output.Logging;

import java.util.List;

public class HikariLoggingAppender implements Appender<ILoggingEvent> {

    static final String NAME = "io.supertokens.storage.mysql.HikariLoggingAppender";

    private final Start start;

    private Context context;

    HikariLoggingAppender(Start start) {
        super();
        this.start = start;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void addStatus(Status status) {

    }

    @Override
    public void addInfo(String msg) {
    }

    @Override
    public void addInfo(String msg, Throwable ex) {

    }

    @Override
    public void addWarn(String msg) {

    }

    @Override
    public void addWarn(String msg, Throwable ex) {

    }

    @Override
    public void addError(String msg) {
    }

    @Override
    public void addError(String msg, Throwable ex) {
    }

    @Override
    public void addFilter(Filter<ILoggingEvent> newFilter) {
    }

    @Override
    public void clearAllFilters() {
    }

    @Override
    public List<Filter<ILoggingEvent>> getCopyOfAttachedFiltersList() {
        return null;
    }

    @Override
    public FilterReply getFilterChainDecision(ILoggingEvent event) {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public void doAppend(ILoggingEvent event) throws LogbackException {
        if (event.getLevel() == Level.ERROR) {
            Logging.error(start, event.getFormattedMessage(), false);
        } else if (event.getLevel() == Level.WARN) {
            Logging.warn(start, event.getFormattedMessage());
        } else {
            Logging.debug(start, event.getFormattedMessage());
        }

    }

}
