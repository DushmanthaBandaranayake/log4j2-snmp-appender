package org.dush.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sends real traps through a Log4j configuration to an in-process snmp4j trap receiver.
 */
class SNMPTrapAppenderEndToEndTest
{
	private static final String APP_OID = "1.3.6.1.4.1.24.12.10.22.64";

	private Snmp receiver;
	private int receiverPort;
	private final BlockingQueue<CommandResponderEvent> traps = new LinkedBlockingQueue<>();

	@BeforeEach void startReceiver() throws Exception
	{
		DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping( new UdpAddress( InetAddress.getLoopbackAddress(), 0 ) );
		receiver = new Snmp( transport );
		receiver.addCommandResponder( new CommandResponder()
		{
			@Override public void processPdu( CommandResponderEvent event )
			{
				traps.add( event );
			}
		} );
		receiver.listen();
		receiverPort = transport.getListenAddress().getPort();
	}

	@AfterEach void stopReceiver() throws Exception
	{
		receiver.close();
	}

	@Test void sendsV2cAndV1TrapsFromConfiguredPortAndReleasesItOnStop() throws Exception
	{
		int localPort = freeUdpPort();
		LoggerContext context = new LoggerContext( "snmp-e2e" );
		context.start( configuration( localPort ) );
		try
		{
			Logger logger = context.getLogger( "TEST" );
			logger.error( "Ouch!" );
			logger.info( "with exception", new IllegalStateException( "bad state" ) );

			List<CommandResponderEvent> received = new ArrayList<>();
			for ( int i = 0; i < 4; i++ )
			{
				CommandResponderEvent event = traps.poll( 10, TimeUnit.SECONDS );
				assertNotNull( event, "expected 4 traps, got " + received.size() );
				received.add( event );
			}

			int v1 = 0;
			int v2 = 0;
			for ( CommandResponderEvent event : received )
			{
				assertEquals( localPort, ( ( UdpAddress ) event.getPeerAddress() ).getPort(), "traps must be sent from LocalTrapSendPort" );
				assertEquals( "secret", new String( event.getSecurityName() ) );
				PDU pdu = event.getPDU();
				String message = messageOf( pdu );
				assertTrue( message.equals( "ERROR TEST - Ouch!" ) || message.equals( "INFO TEST - with exception" ), message );

				if ( event.getMessageProcessingModel() == SnmpConstants.version1 )
				{
					v1++;
					assertEquals( 42, ( ( PDUv1 ) pdu ).getSpecificTrap() );
					assertEquals( 6, ( ( PDUv1 ) pdu ).getGenericTrap() );
				}
				else
				{
					v2++;
					assertEquals( SnmpConstants.sysUpTime, pdu.get( 0 ).getOid() );
				}

				if ( message.startsWith( "INFO" ) )
				{
					assertTrue( containsValue( pdu, "java.lang.IllegalStateException: bad state" ), "stack trace varbinds missing" );
				}
			}
			assertEquals( 2, v1 );
			assertEquals( 2, v2 );
		}
		finally
		{
			context.stop();
		}

		// the appender's socket is closed on stop, so the port can be bound again
		try ( DatagramSocket socket = new DatagramSocket( new InetSocketAddress( "0.0.0.0", localPort ) ) )
		{
			assertTrue( socket.isBound() );
		}
	}

	private Configuration configuration( int localPort )
	{
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		builder.setStatusLevel( Level.WARN );
		builder.setConfigurationName( "snmp-e2e" );
		AppenderComponentBuilder appender = builder.newAppender( "SNMP", "SNMPTrapAppender" )
				.addAttribute( "SNMPVersions", "2c, 1" )
				.addAttribute( "ManagementHost", "127.0.0.1" )
				.addAttribute( "ManagementHostTrapListenPort", String.valueOf( receiverPort ) )
				.addAttribute( "EnterpriseOID", "1.3.6.1.4.1.24.0" )
				.addAttribute( "LocalIPAddress", "127.0.0.1" )
				.addAttribute( "LocalTrapSendPort", String.valueOf( localPort ) )
				.addAttribute( "GenericTrapType", "6" )
				.addAttribute( "SpecificTrapType", "42" )
				.addAttribute( "CommunityString", "secret" )
				.addAttribute( "ForwardStackTraceWithTrap", "true" )
				.addAttribute( "ApplicationTrapOID", APP_OID )
				.add( builder.newLayout( "PatternLayout" ).addAttribute( "pattern", "%p %c - %m" ).addAttribute( "alwaysWriteExceptions", false ) );
		builder.add( appender );
		builder.add( builder.newLogger( "TEST", Level.ALL ).add( builder.newAppenderRef( "SNMP" ) ).addAttribute( "additivity", false ) );
		builder.add( builder.newRootLogger( Level.OFF ) );
		return builder.build( false );
	}

	private static String messageOf( PDU pdu )
	{
		for ( int i = 0; i < pdu.size(); i++ )
		{
			if ( pdu.get( i ).getOid().toDottedString().equals( APP_OID ) )
			{
				return pdu.get( i ).getVariable().toString();
			}
		}
		throw new AssertionError( "no application varbind in " + pdu );
	}

	private static boolean containsValue( PDU pdu, String value )
	{
		for ( int i = 0; i < pdu.size(); i++ )
		{
			if ( new OctetString( value ).equals( pdu.get( i ).getVariable() ) )
			{
				return true;
			}
		}
		return false;
	}

	private static int freeUdpPort() throws Exception
	{
		try ( DatagramSocket socket = new DatagramSocket( 0 ) )
		{
			return socket.getLocalPort();
		}
	}
}
