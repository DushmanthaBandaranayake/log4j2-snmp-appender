package org.dush.log4j2;

import org.apache.logging.log4j.LogManager;

/**
 * Run this to debug the appender manually. Uses src/test/resources/log4j2.xml, which sends traps to 127.0.0.1:162.
 */
public class Main
{
	public static void main( String[] args )
	{
		LogManager.getLogger( "TEST" ).error( "Ouch!" );
		methodOne();
	}

	private static void methodOne()
	{
		LogManager.getLogger( "TEST" ).info( "Exception test message", new NullPointerException( "NPE exception for testing" ) );
	}
}
