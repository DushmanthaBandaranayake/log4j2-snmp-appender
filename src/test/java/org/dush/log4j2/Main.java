package org.dush.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.io.Serializable;

/**
 * Run this to debug the appender
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
