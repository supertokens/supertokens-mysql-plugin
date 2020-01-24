package io.supertokens.storage.mysql.output;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.EncoderBase;
import io.supertokens.storage.mysql.Start;

import java.nio.charset.StandardCharsets;

class LayoutWrappingEncoder extends EncoderBase<ILoggingEvent> {

    private Layout<ILoggingEvent> layout;

    LayoutWrappingEncoder(Start start) {
        layout = new CustomLayout(start);
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        String txt = layout.doLayout(event);
        return convertToBytes(txt);
    }

    private byte[] convertToBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }
}
