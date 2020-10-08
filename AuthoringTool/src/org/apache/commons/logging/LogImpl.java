package org.apache.commons.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple logging interface abstracting logging APIs. In order to be
 * instantiated successfully by {@link LogFactory}, classes that implement this
 * interface must have a constructor that takes a single String parameter
 * representing the "name" of this Log.
 * <p>
 * The six logging levels used by <code>Log</code> are (in order):
 * <ol>
 * <li>trace (the least serious)</li>
 * <li>debug</li>
 * <li>info</li>
 * <li>warn</li>
 * <li>error</li>
 * <li>fatal (the most serious)</li>
 * </ol>
 * The mapping of these log levels to the concepts used by the underlying
 * logging system is implementation dependent. The implementation should ensure,
 * though, that this ordering behaves as expected.
 * <p>
 * Performance is often a logging concern. By examining the appropriate
 * property, a component can avoid expensive operations (producing information
 * to be logged).
 * <p>
 * For example, <code><pre>
 *    if (log.isDebugEnabled()) {
 *        ... do something expensive ...
 *        log.debug(theResult);
 *    }
 * </pre></code>
 * <p>
 * Configuration of the underlying logging system will generally be done
 * external to the Logging APIs, through whatever mechanism is supported by that
 * system.
 * 
 * @version $Id: Log.java 1432663 2013-01-13 17:24:18Z tn $
 */
public class LogImpl implements Log {
	private final Logger logger;

	LogImpl(Class<?> klass) {
		logger = Logger.getLogger(klass.getName());
	}

	// ----------------------------------------------------- Logging Properties

	/**
	 * Is debug logging currently enabled?
	 * <p>
	 * Call this method to prevent having to perform expensive operations (for
	 * example, <code>String</code> concatenation) when the log level is more than
	 * debug.
	 * 
	 * @return true if debug is enabled in the underlying logger.
	 */
	@Override
	public boolean isDebugEnabled() {
		return logger.isLoggable(Level.FINE);
	}

	/**
	 * Is error logging currently enabled?
	 * <p>
	 * Call this method to prevent having to perform expensive operations (for
	 * example, <code>String</code> concatenation) when the log level is more than
	 * error.
	 * 
	 * @return true if error is enabled in the underlying logger.
	 */
	@Override
	public boolean isErrorEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}

	/**
	 * Is fatal logging currently enabled?
	 * <p>
	 * Call this method to prevent having to perform expensive operations (for
	 * example, <code>String</code> concatenation) when the log level is more than
	 * fatal.
	 * 
	 * @return true if fatal is enabled in the underlying logger.
	 */
	@Override
	public boolean isFatalEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}

	/**
	 * Is info logging currently enabled?
	 * <p>
	 * Call this method to prevent having to perform expensive operations (for
	 * example, <code>String</code> concatenation) when the log level is more than
	 * info.
	 * 
	 * @return true if info is enabled in the underlying logger.
	 */
	@Override
	public boolean isInfoEnabled() {
		return logger.isLoggable(Level.INFO);
	}

	/**
	 * Is trace logging currently enabled?
	 * <p>
	 * Call this method to prevent having to perform expensive operations (for
	 * example, <code>String</code> concatenation) when the log level is more than
	 * trace.
	 * 
	 * @return true if trace is enabled in the underlying logger.
	 */
	@Override
	public boolean isTraceEnabled() {
		return logger.isLoggable(Level.FINER);
	}

	/**
	 * Is warn logging currently enabled?
	 * <p>
	 * Call this method to prevent having to perform expensive operations (for
	 * example, <code>String</code> concatenation) when the log level is more than
	 * warn.
	 * 
	 * @return true if warn is enabled in the underlying logger.
	 */
	@Override
	public boolean isWarnEnabled() {
		return logger.isLoggable(Level.WARNING);
	}

	// -------------------------------------------------------- Logging Methods

	/**
	 * Log a message with trace log level.
	 * 
	 * @param message log this message
	 */
	@Override
	public void trace(Object message) {
		logger.log(Level.FINER, message.toString());
	}

	/**
	 * Log an error with trace log level.
	 * 
	 * @param message log this message
	 * @param t       log this cause
	 */
	@Override
	public void trace(Object message, Throwable t) {
		logger.log(Level.FINER, message.toString(), t);
	}

	/**
	 * Log a message with debug log level.
	 * 
	 * @param message log this message
	 */
	@Override
	public void debug(Object message) {
		logger.log(Level.FINE, message.toString());
	}

	/**
	 * Log an error with debug log level.
	 * 
	 * @param message log this message
	 * @param t       log this cause
	 */
	@Override
	public void debug(Object message, Throwable t) {
		logger.log(Level.FINE, message.toString(), t);
	}

	/**
	 * Log a message with info log level.
	 * 
	 * @param message log this message
	 */
	@Override
	public void info(Object message) {
		logger.log(Level.INFO, message.toString());
	}

	/**
	 * Log an error with info log level.
	 * 
	 * @param message log this message
	 * @param t       log this cause
	 */
	@Override
	public void info(Object message, Throwable t) {
		logger.log(Level.INFO, message.toString(), t);
	}

	/**
	 * Log a message with warn log level.
	 * 
	 * @param message log this message
	 */
	@Override
	public void warn(Object message) {
		logger.log(Level.WARNING, message.toString());
	}

	/**
	 * Log an error with warn log level.
	 * 
	 * @param message log this message
	 * @param t       log this cause
	 */
	@Override
	public void warn(Object message, Throwable t) {
		logger.log(Level.WARNING, message.toString(), t);
	}

	/**
	 * Log a message with error log level.
	 * 
	 * @param message log this message
	 */
	@Override
	public void error(Object message) {
		logger.log(Level.SEVERE, message.toString());
	}

	/**
	 * Log an error with error log level.
	 * 
	 * @param message log this message
	 * @param t       log this cause
	 */
	@Override
	public void error(Object message, Throwable t) {
		logger.log(Level.SEVERE, message.toString(), t);
	}

	/**
	 * Log a message with fatal log level.
	 * 
	 * @param message log this message
	 */
	@Override
	public void fatal(Object message) {
		logger.log(Level.SEVERE, message.toString());
	}

	/**
	 * Log an error with fatal log level.
	 * 
	 * @param message log this message
	 * @param t       log this cause
	 */
	@Override
	public void fatal(Object message, Throwable t) {
		logger.log(Level.SEVERE, message.toString(), t);
	}
}
