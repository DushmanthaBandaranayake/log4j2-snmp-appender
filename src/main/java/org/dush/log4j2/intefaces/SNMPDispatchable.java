package org.dush.log4j2.intefaces;

/**
 * Can have custom implemention by implementing this calss.
 * For example if the SNMP message should be send using different SNMP library/vendor such as wengsoft net-snmp java library
 * implement this interface and add the fully qualified name of the implementation into log4j configuration. And note any third party
 * libraries should be provided also if need.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public interface SNMPDispatchable
{
	//@formatter:off
	/**
	 *
	 * sends SNMP request.
	 * @param message         Log message coming from logger,
	 *                        Eg. if log line in java code is like <l>logger.info(this is a log message)</il>
	 *                        then the @param would be like <i>2018-11-23 [main.test.TestClass][info] this is a log message</i>.
	 *
	 * @param exceptionStackTrace
	 * 						  When logger has an {@link Exception e}, the stackTrace of 'e' is represented by exceptionStackTrace.
	 * 						  example use of this could be, to put exception stack trace into SNMP varbings.
	 *                        pass null if dont want process exceptions.
	 *
	 */
	//@formatter:on
	void dispatch( String message, String[] exceptionStackTrace ) throws Exception;

	/**
	 * implement this method to to close resources.
	 * For example it implementing class use TCP connection, connection close method can be used here.
	 *
	 * @throws Exception can exceptions when releasing resources. Eg. IOException
	 */
	void releaseResources() throws Exception;

}
