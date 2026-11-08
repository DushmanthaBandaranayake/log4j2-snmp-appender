package org.dush.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.dush.log4j2.interfaces.SNMPDispatchable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SNMPTrapAppenderPluginBuilderTest
{
	@Test void parseVersionsTrimsIgnoresCaseAndDropsDuplicates()
	{
		assertEquals( Arrays.asList( "2c", "1" ), SNMPTrapAppenderPluginBuilder.parseVersions( " 2C , 1,2c, 2 ,1" ) );
		assertEquals( Collections.singletonList( "2c" ), SNMPTrapAppenderPluginBuilder.parseVersions( "2" ) );
	}

	@Test void parseVersionsRejectsUnknownOrEmpty()
	{
		assertThrows( IllegalArgumentException.class, () -> SNMPTrapAppenderPluginBuilder.parseVersions( "1,3" ) );
		assertThrows( IllegalArgumentException.class, () -> SNMPTrapAppenderPluginBuilder.parseVersions( " , " ) );
		assertThrows( IllegalArgumentException.class, () -> SNMPTrapAppenderPluginBuilder.parseVersions( null ) );
	}

	@Test void parseIntHandlesDefaultsRangesAndGarbage()
	{
		assertEquals( 7, SNMPTrapAppenderPluginBuilder.parseInt( null, "X", 7, 0, 10 ) );
		assertEquals( 7, SNMPTrapAppenderPluginBuilder.parseInt( "  ", "X", 7, 0, 10 ) );
		assertEquals( 3, SNMPTrapAppenderPluginBuilder.parseInt( " 3 ", "X", 7, 0, 10 ) );
		assertThrows( IllegalArgumentException.class, () -> SNMPTrapAppenderPluginBuilder.parseInt( "11", "X", 7, 0, 10 ) );
		assertThrows( IllegalArgumentException.class, () -> SNMPTrapAppenderPluginBuilder.parseInt( "abc", "X", 7, 0, 10 ) );
	}

	@Test void configHolderGetsParsedValues()
	{
		ConfigHolder config = validBuilder()
				.setSpecificTrapType( "123" )
				.setGenericTrapType( "6" )
				.setLocalTrapSendPort( "1610" )
				.setForwardStackTraceWithTrap( "TRUE" )
				.setSnmpVersions( "1, 2c" )
				.toConfigHolder();

		assertEquals( 123, config.getSpecificTrapType() );
		assertEquals( 6, config.getGenericTrapType() );
		assertEquals( 1610, config.getLocalTrapSendPort() );
		assertTrue( config.isForwardStackTraceWithTrap() );
		assertEquals( Arrays.asList( "1", "2c" ), config.getSnmpVersions() );
		assertEquals( "127.0.0.1/162", config.getManagementHostPort() );
	}

	@Test void buildReturnsNullForInvalidConfig()
	{
		assertNull( validBuilder().setSnmpVersions( "3" ).build() );
		assertNull( validBuilder().setSnmpVersions( null ).build() );
		assertNull( validBuilder().setGenericTrapType( "7" ).build() );
		assertNull( validBuilder().setSpecificTrapType( "not-a-number" ).build() );
		assertNull( validBuilder().setEnterpriseOID( null ).build() );
		assertNull( validBuilder().setManagementHost( null ).build() );
	}

	@Test void buildUsesDefaultLayoutWhenNoneIsSet()
	{
		SNMPTrapAppender appender = validBuilder().build();
		assertNotNull( appender );
		try
		{
			assertInstanceOf( PatternLayout.class, appender.getLayout() );
			assertEquals( 2, appender.getDispatchers().size() );
		}
		finally
		{
			appender.stop();
		}
	}

	@Test void customDispatcherReceivesMessagesAndIsReleased()
	{
		RecordingDispatcher.INSTANCES.clear();
		SNMPTrapAppender appender = SNMPTrapAppender.newBuilder()
				.setName( "custom" )
				.setLayout( PatternLayout.newBuilder().withPattern( "%p %m" ).withAlwaysWriteExceptions( false ).build() )
				.setDispatcherClass( RecordingDispatcher.class.getName() + " , " + NoArgDispatcher.class.getName() )
				.setSnmpVersions( "2c" )
				.setForwardStackTraceWithTrap( "true" )
				.build();
		assertNotNull( appender );
		appender.start();

		appender.append( event( "hello", null ) );
		appender.append( event( "boom", new IllegalStateException( "bad" ) ) );

		RecordingDispatcher recorder = RecordingDispatcher.INSTANCES.get( 0 );
		assertEquals( Collections.singletonList( "2c" ), recorder.config.getSnmpVersions() );
		assertEquals( Arrays.asList( "INFO hello", "INFO boom" ), recorder.messages );
		assertNull( recorder.stackTraces.get( 0 ) );
		assertEquals( "java.lang.IllegalStateException: bad", recorder.stackTraces.get( 1 )[0] );
		assertInstanceOf( NoArgDispatcher.class, appender.getDispatchers().get( 1 ) );

		assertFalse( recorder.released );
		appender.stop();
		assertTrue( recorder.released );
	}

	@Test void stackTraceIsNotForwardedUnlessEnabled()
	{
		RecordingDispatcher.INSTANCES.clear();
		SNMPTrapAppender appender = SNMPTrapAppender.newBuilder()
				.setName( "custom" )
				.setLayout( PatternLayout.newBuilder().withPattern( "%m" ).build() )
				.setDispatcherClass( RecordingDispatcher.class.getName() )
				.build();
		assertNotNull( appender );
		appender.append( event( "boom", new IllegalStateException( "bad" ) ) );
		assertEquals( 1, RecordingDispatcher.INSTANCES.get( 0 ).stackTraces.size() );
		assertNull( RecordingDispatcher.INSTANCES.get( 0 ).stackTraces.get( 0 ) );
		appender.stop();
	}

	@Test void customDispatcherMustImplementInterface()
	{
		assertNull( SNMPTrapAppender.newBuilder().setName( "x" ).setDispatcherClass( String.class.getName() ).build() );
		assertNull( SNMPTrapAppender.newBuilder().setName( "x" ).setDispatcherClass( "no.such.Dispatcher" ).build() );
	}

	@SuppressWarnings("deprecation")
	@Test void deprecatedInterfaceIsStillAccepted()
	{
		SNMPTrapAppender appender = SNMPTrapAppender.newBuilder().setName( "x" ).setDispatcherClass( LegacyDispatcher.class.getName() ).build();
		assertNotNull( appender );
		appender.stop();
	}

	private static SNMPTrapAppenderPluginBuilder validBuilder()
	{
		return SNMPTrapAppender.newBuilder()
				.setName( "snmp" )
				.setSnmpVersions( "2c,1" )
				.setManagementHost( "127.0.0.1" )
				.setManagementHostTrapListenPort( "162" )
				.setEnterpriseOID( "1.3.6.1.4.1.24.0" )
				.setApplicationTrapOID( "1.3.6.1.4.1.24.12.10.22.64" )
				.setLocalIPAddress( "127.0.0.1" );
	}

	static LogEvent event( String message, Throwable thrown )
	{
		return Log4jLogEvent.newBuilder()
				.setLoggerName( "TEST" )
				.setLevel( Level.INFO )
				.setMessage( new SimpleMessage( message ) )
				.setThrown( thrown )
				.build();
	}

	public static class RecordingDispatcher implements SNMPDispatchable
	{
		static final List<RecordingDispatcher> INSTANCES = new ArrayList<>();

		final ConfigHolder config;
		final List<String> messages = new ArrayList<>();
		final List<String[]> stackTraces = new ArrayList<>();
		boolean released;

		public RecordingDispatcher( ConfigHolder config )
		{
			this.config = config;
			INSTANCES.add( this );
		}

		@Override public void dispatch( String message, String[] exceptionStackTrace )
		{
			messages.add( message );
			stackTraces.add( exceptionStackTrace );
		}

		@Override public void releaseResources()
		{
			released = true;
		}
	}

	public static class NoArgDispatcher implements SNMPDispatchable
	{
		@Override public void dispatch( String message, String[] exceptionStackTrace )
		{
		}

		@Override public void releaseResources()
		{
		}
	}

	@SuppressWarnings("deprecation")
	public static class LegacyDispatcher implements org.dush.log4j2.intefaces.SNMPDispatchable
	{
		@Override public void dispatch( String message, String[] exceptionStackTrace )
		{
		}

		@Override public void releaseResources()
		{
		}
	}
}
