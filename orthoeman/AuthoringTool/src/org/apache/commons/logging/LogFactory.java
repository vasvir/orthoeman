package org.apache.commons.logging;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogFactory {
    private static Formatter formatter = new SimpleFormatter();
    private static Level level = Level.ALL;

    static {
        final Logger logger = Logger.getLogger("");

        for (final Handler handler : logger.getHandlers()) {
            handler.setFormatter(formatter);
            handler.setLevel(level);
        }
    }

    public static Log getLog(Class<?> klass) {
        return new LogImpl(klass);
    }
}
