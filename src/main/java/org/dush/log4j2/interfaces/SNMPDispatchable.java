package org.dush.log4j2.interfaces;

/**
 * Extension point for sending SNMP traps with a different SNMP library or vendor.
 * <p>
 * By default the appender uses the snmp4j based dispatchers. To use another library, implement this interface and
 * set the fully qualified class name in the {@code <DispatcherClass>} element of the appender configuration. The
 * class must have either a public constructor taking a {@link org.dush.log4j2.ConfigHolder} or a public no-arg
 * constructor, and it (plus any third party libraries it needs) must be on the classpath.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public interface SNMPDispatchable
{
	//@formatter:off
	/**
	 * Sends an SNMP trap.
	 *
	 * @param message             Log message formatted by the appender's layout.
	 *                            E.g. if the log line in java code is {@code logger.info("this is a log message")}
	 *                            then the message would be like <i>2018-11-23 [main.test.TestClass][info] this is a log message</i>.
	 *
	 * @param exceptionStackTrace When the log event has a {@link Throwable}, its stack trace (type and message, frames
	 *                            and causes), one entry per line. Can be used, for example, to put the stack trace into
	 *                            SNMP varbinds. {@code null} when the event has no throwable or
	 *                            {@code ForwardStackTraceWithTrap} is not enabled.
	 *
	 * @throws Exception when the trap cannot be sent
	 */
	//@formatter:on
	void dispatch( String message, String[] exceptionStackTrace ) throws Exception;

	/**
	 * Releases any resources held by this dispatcher, e.g. sockets or connections. Called when the appender is
	 * stopped (Log4j shutdown or reconfiguration).
	 *
	 * @throws Exception when releasing resources fails. E.g. IOException
	 */
	void releaseResources() throws Exception;

}
