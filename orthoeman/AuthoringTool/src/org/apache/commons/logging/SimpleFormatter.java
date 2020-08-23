package org.apache.commons.logging;

import java.util.Date;
import java.util.logging.LogRecord;

import com.google.gwt.logging.impl.FormatterImpl;
import com.google.gwt.logging.impl.StackTracePrintStream;

public class SimpleFormatter extends FormatterImpl {
    private static final StringBuilder sb = new StringBuilder();

    /**
     * Format the given LogRecord.
     * 
     * @param record
     *            the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public String format(LogRecord record) {
        synchronized(sb) {
            sb.setLength(0);
            sb.append(record.getLevel().getName());
            sb.append(" ");
            sb.append(new Date(record.getMillis()).toString());
            sb.append(": ");
            sb.append(record.getLoggerName());
            sb.append(": ");
            sb.append(record.getMessage());

            if (record.getThrown() != null) {
                record.getThrown()
                        .printStackTrace(new StackTracePrintStream(sb));
            }

            return sb.toString();
        }
    }
}
