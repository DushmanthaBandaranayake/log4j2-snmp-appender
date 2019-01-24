package org.dush.log4j2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.dush.log4j2.intefaces.SNMPDispatchable;
import org.dush.log4j2.vendors.snmp4j.Snmp4JTrapDispatcherV1;
import org.dush.log4j2.vendors.snmp4j.Snmp4JTrapDispatcherV2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public class SNMPTrapAppenderPluginBuilder
		extends org.apache.logging.log4j.core.appender.AbstractAppender.Builder
		implements org.apache.logging.log4j.core.util.Builder<SNMPTrapAppender>
{
	/**
	 * SNMP version. Applicable values are
	 * <l><li>1</li><li>2c</li></l>.
	 */
	@PluginBuilderAttribute("snmpVersions")
	private String snmpVersions;

	/**
	 * IP address of the remote host that traps are sent into.
	 */
	@PluginBuilderAttribute("ManagementHost")
	private String managementHost;

	/**
	 * listening port for SNMP traps in management host. could be any TCP/IP port.
	 * The standard is 162.
	 */
	@PluginBuilderAttribute("ManagementHostTrapListenPort")
	private String managementHostTrapListenPort;

	/**
	 * Enterprise OID that will be sent in the SNMP PDU.
	 * Ex: 1.3.6.1.2.1.1.2.0 points to the standard sysObjectID of the "systemName" node of the standard system MIB.
	 */
	@PluginBuilderAttribute("EnterpriseOID")
	private String enterpriseOID;

	/**
	 * IP address of the host that is using this appender to send SNMP traps.
	 * This address will be encoded in the SNMP PDU.
	 */
	@PluginBuilderAttribute("LocalIPAddress")
	private String localIPAddress;

	/**
	 * Value of the port that will be used to send traps out from the local host.The standard is 161.
	 */
	@PluginBuilderAttribute("LocalTrapSendPort")
	private String localTrapSendPort;

	/**
	 * An INTEGER in (value between -128 to 127)
	 */
	@PluginBuilderAttribute("SpecificTrapType")
	private String specificTrapType;

	/**
	 * GenericTrapType int, Applicable for SNMP v1
	 * Value	Type
	 * 0	    coldStart
	 * 1	    warmStart
	 * 2	    linkDown
	 * 3	    linkUp
	 * 4	    authenticationFailure
	 * 5	    egpNeighborLoss
	 * 6	    enterpriseSpecific
	 */
	@PluginBuilderAttribute("GenericTrapType")
	private String genericTrapType;

	/**
	 * community string for the SNMP session. E.g. "public".
	 */
	@PluginBuilderAttribute("CommunityString")
	private String communityString;

	/**
	 * Add each line of exception stack trace in to separate varbindings.
	 */
	@PluginBuilderAttribute("ForwardStackTraceWithTrap")
	private String forwardStackTraceWithTrap;

	/**
	 * OID that will be sent in the SNMP PDU for this app.
	 * variable binding for Enterprise Specific objects, this should be available in MIB -->
	 */
	@PluginBuilderAttribute("ApplicationTrapOID")
	private String applicationTrapOID;

	/**
	 * if true prints debugging information on console(system.print())
	 */
	@PluginBuilderAttribute("verbose")
	private boolean verbose;

	@Override
	@SuppressWarnings("unchecked")
	public SNMPTrapAppender build()
	{
		ConfigHolder configHolder = new ConfigHolder();
		configHolder.setApplicationTrapOID( applicationTrapOID );
		configHolder.setCommunityString( communityString );
		configHolder.setEnterpriseOID( enterpriseOID );
		configHolder.setForwardStackTraceWithTrap( forwardStackTraceWithTrap );
		configHolder.setGenericTrapType( genericTrapType );
		configHolder.setLocalIPAddress( localIPAddress );
		configHolder.setLocalTrapSendPort( localTrapSendPort );
		configHolder.setManagementHost( managementHost );
		configHolder.setMgtHostPort( managementHostTrapListenPort );
		configHolder.setSpecific( Boolean.parseBoolean( specificTrapType ) );
		configHolder.setVerbose( verbose );
		Util.DEBUG_ON = verbose;
		Util.FORWARD_STACKTRACE_WITH_TRAP = "true".equalsIgnoreCase( forwardStackTraceWithTrap );

		try
		{
			return new SNMPTrapAppender( getName(), getFilter(), getLayout(), buildDispatchers( configHolder ) );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			return null;
		}

	}

	private List<SNMPDispatchable> buildDispatchers( ConfigHolder configHolder ) throws Exception
	{
		List<SNMPDispatchable> snmpDispatchables = new ArrayList<>();
		if ( snmpVersions != null )
		{
			for ( String version : snmpVersions.split( "," ) )
			{
				switch ( version )
				{
					case "1":
						snmpDispatchables.add( getImplementationsV1( configHolder ) );
						break;
					case "2c":
						snmpDispatchables.add( getImplementationsV2( configHolder ) );
						break;
					default:
						snmpDispatchables.add( getImplementationsV1( configHolder ) );
				}
			}

		}
		else
		{
			throw new Exception( "Invalid of null argument for <SNMPVersions> " + snmpVersions + "<\\SNMPVersions>" );
		}
		return snmpDispatchables;
	}

	private SNMPDispatchable getImplementationsV2( ConfigHolder configHolder ) throws IOException
	{
		//
		return new Snmp4JTrapDispatcherV2( configHolder );
	}

	private SNMPDispatchable getImplementationsV1( ConfigHolder configHolder ) throws IOException
	{
		//
		return new Snmp4JTrapDispatcherV1( configHolder );
	}

	/**
	 * Provide utility methods.
	 */
	final public static class Util
	{
		private static boolean DEBUG_ON;
		private static boolean FORWARD_STACKTRACE_WITH_TRAP;

		/**
		 * When parameter "<verbose>true</verbose>" in log4j2 configuration is true, debug information are printed out
		 * to the console. Message @param msg is appnded by prefix "SNMP_APPENDER: ".
		 *
		 * @param msg String to print
		 */
		public static void printToConsole( String msg )
		{
			if ( DEBUG_ON )
			{
				System.out.println( "SNMP_APPENDER: " + msg );
			}
		}

		/**
		 * When parameter "<verbose>true</verbose>" in log4j2 configuration is true, debug information are printed out
		 * to the console. Message @param msg is appnded by prefix "SNMP_APPENDER: ".
		 *
		 * @param msg String to print
		 * @param e   Exception to be printed
		 */
		public static void printToConsole( String msg, Exception e )
		{
			if ( DEBUG_ON )
			{
				System.out.println( "SNMP_APPENDER: " + msg );
				e.printStackTrace();
			}
		}

		public static String[] stripThrowableStack( LogEvent event )
		{
			String[] outArray = null;

			if ( FORWARD_STACKTRACE_WITH_TRAP && event.getThrown() != null && event.getThrown().getStackTrace() != null )
			{
				StackTraceElement[] stackTrace = event.getThrown().getStackTrace();
				outArray = new String[stackTrace.length];
				for ( int i = 0; i < stackTrace.length; i++ )
				{
					outArray[i] = stackTrace[i].toString();
				}
			}
			return outArray;
		}
	}

}
