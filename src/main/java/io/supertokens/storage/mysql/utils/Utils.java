package io.supertokens.storage.mysql.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Utils {
    public static String exceptionStacktraceToString(Exception e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        e.printStackTrace(ps);
        ps.close();
        return baos.toString();
    }
}
