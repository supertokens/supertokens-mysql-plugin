package io.supertokens.storage.mysql.output;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import io.supertokens.storage.mysql.Start;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

class CustomLayout extends LayoutBase<ILoggingEvent> {

    private final Start start;

    CustomLayout(Start start) {
        super();
        this.start = start;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder sbuf = new StringBuilder();

        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        sbuf.append(dateFormat.format(new Date(event.getTimeStamp())));
        sbuf.append(" | ");

        sbuf.append(event.getLevel());
        sbuf.append(" | ");

        sbuf.append("pid: ");
        sbuf.append(start.getProcessId());
        sbuf.append(" | ");

        sbuf.append("[");
        sbuf.append(event.getThreadName());
        sbuf.append("] thread");
        sbuf.append(" | ");

        sbuf.append(event.getCallerData()[1]);
        sbuf.append(" | ");

        sbuf.append(event.getFormattedMessage());
        sbuf.append(CoreConstants.LINE_SEPARATOR);
        sbuf.append(CoreConstants.LINE_SEPARATOR);

        return sbuf.toString();
    }
}
