package org.dush.log4j2;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.dush.log4j2.interfaces.SNMPDispatchable;
import org.dush.log4j2.vendors.snmp4j.Snmp4JSession;
import org.dush.log4j2.vendors.snmp4j.Snmp4JTrapDispatcherV1;
import org.dush.log4j2.vendors.snmp4j.Snmp4JTrapDispatcherV2;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds {@link SNMPTrapAppender} from the Log4j2 configuration.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public class SNMPTrapAppenderPluginBuilder
		extends AbstractAppender.Builder<SNMPTrapAppenderPluginBuilder>
		implements org.apache.logging.log4j.core.util.Builder<SNMPTrapAppender>
{
	private static final Logger LOGGER = StatusLogger.getLogger();

	static final String VERSION_1 = "1";
	static final String VERSION_2C = "2c";

	/**
	 * Comma separated SNMP versions. Applicable values are {@code 1}, {@code 2c} (or {@code 2}).
	 */
	@PluginBuilderAttribute("snmpVersions")
	private String snmpVersions;

	/**
	 * IP address of the remote host that traps are sent into.
	 */
	@PluginBuilderAttribute("ManagementHost")
	private String managementHost;

	/**
	 * Listening port for SNMP traps in management host. The standard is 162.
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
	 * Local UDP port that traps are sent from. Empty or 0 means an ephemeral port.
	 */
	@PluginBuilderAttribute("LocalTrapSendPort")
	private String localTrapSendPort;

	/**
	 * Specific trap code for SNMP v1, an INTEGER (Integer32).
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
	 * Community string for the SNMP session. Defaults to "public".
	 */
	@PluginBuilderAttribute("CommunityString")
	private String communityString;

	/**
	 * Add each line of the exception stack trace into separate varbinds.
	 */
	@PluginBuilderAttribute("ForwardStackTraceWithTrap")
	private String forwardStackTraceWithTrap;

	/**
	 * OID that will be sent in the SNMP PDU for this app.
	 * Variable binding for Enterprise Specific objects, this should be available in MIB.
	 */
	@PluginBuilderAttribute("ApplicationTrapOID")
	private String applicationTrapOID;

	/**
	 * If true prints debugging information on the console (System.out).
	 */
	@PluginBuilderAttribute("verbose")
	private boolean verbose;

	/**
	 * Optional comma separated fully qualified class names of custom {@link SNMPDispatchable} implementations.
	 * When set, these are used instead of the built-in snmp4j dispatchers.
	 */
	@PluginBuilderAttribute("DispatcherClass")
	private String dispatcherClass;

	@Override
	public SNMPTrapAppender build()
	{
		try
		{
			Layout<? extends Serializable> layout = getLayout();
			if ( layout == null )
			{
				LOGGER.warn( "No layout set for SNMPTrapAppender [{}], using the default PatternLayout", getName() );
				layout = PatternLayout.createDefaultLayout( getConfiguration() );
			}
			ConfigHolder configHolder = toConfigHolder();
			List<SNMPDispatchable> dispatchers = buildDispatchers( configHolder );
			return new SNMPTrapAppender( getName(), getFilter(), layout, isIgnoreExceptions(), getPropertyArray(),
					dispatchers, configHolder );
		}
		catch ( Exception e )
		{
			LOGGER.error( "Unable to create SNMPTrapAppender [{}]: {}", getName(), e.getMessage(), e );
			return null;
		}
	}

	ConfigHolder toConfigHolder()
	{
		ConfigHolder configHolder = new ConfigHolder();
		configHolder.setApplicationTrapOID( trimToNull( applicationTrapOID ) );
		configHolder.setCommunityString( trimToNull( communityString ) );
		configHolder.setEnterpriseOID( trimToNull( enterpriseOID ) );
		configHolder.setForwardStackTraceWithTrap( "true".equalsIgnoreCase( trimToNull( forwardStackTraceWithTrap ) ) );
		configHolder.setGenericTrapType( parseInt( genericTrapType, "GenericTrapType", 6, 0, 6 ) );
		configHolder.setSpecificTrapType( parseInt( specificTrapType, "SpecificTrapType", 0, Integer.MIN_VALUE, Integer.MAX_VALUE ) );
		configHolder.setLocalIPAddress( trimToNull( localIPAddress ) );
		configHolder.setLocalTrapSendPort( parseInt( localTrapSendPort, "LocalTrapSendPort", 0, 0, 65535 ) );
		configHolder.setManagementHost( trimToNull( managementHost ) );
		configHolder.setMgtHostPort( trimToNull( managementHostTrapListenPort ) );
		configHolder.setVerbose( verbose );
		configHolder.setSnmpVersions( isBlank( snmpVersions ) ? Collections.<String>emptyList() : parseVersions( snmpVersions ) );
		return configHolder;
	}

	private List<SNMPDispatchable> buildDispatchers( ConfigHolder configHolder ) throws Exception
	{
		if ( !isBlank( dispatcherClass ) )
		{
			List<SNMPDispatchable> dispatchers = new ArrayList<>();
			for ( String className : dispatcherClass.split( "," ) )
			{
				if ( !isBlank( className ) )
				{
					dispatchers.add( instantiateDispatcher( className.trim(), configHolder ) );
				}
			}
			return dispatchers;
		}

		if ( configHolder.getSnmpVersions().isEmpty() )
		{
			throw new IllegalArgumentException( "<SNMPVersions> is required, applicable values are 1, 2c" );
		}

		List<SNMPDispatchable> dispatchers = new ArrayList<>();
		Snmp4JSession session = new Snmp4JSession( configHolder.getLocalTrapSendPort() );
		try
		{
			for ( String version : configHolder.getSnmpVersions() )
			{
				dispatchers.add( VERSION_1.equals( version )
						? new Snmp4JTrapDispatcherV1( configHolder, session )
						: new Snmp4JTrapDispatcherV2( configHolder, session ) );
			}
		}
		catch ( RuntimeException e )
		{
			for ( SNMPDispatchable dispatcher : dispatchers )
			{
				dispatcher.releaseResources();
			}
			throw e;
		}
		finally
		{
			// drop the creator's reference; the session stays open while any dispatcher holds it
			session.release();
		}
		return dispatchers;
	}

	static SNMPDispatchable instantiateDispatcher( String className, ConfigHolder configHolder ) throws Exception
	{
		Class<?> type = loadClass( className );
		if ( !SNMPDispatchable.class.isAssignableFrom( type ) )
		{
			throw new IllegalArgumentException( className + " does not implement " + SNMPDispatchable.class.getName() );
		}
		Constructor<?> constructor;
		try
		{
			constructor = type.getConstructor( ConfigHolder.class );
			return ( SNMPDispatchable ) constructor.newInstance( configHolder );
		}
		catch ( NoSuchMethodException e )
		{
			constructor = type.getConstructor();
			return ( SNMPDispatchable ) constructor.newInstance();
		}
	}

	private static Class<?> loadClass( String className ) throws ClassNotFoundException
	{
		ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
		if ( contextLoader != null )
		{
			try
			{
				return Class.forName( className, true, contextLoader );
			}
			catch ( ClassNotFoundException ignored )
			{
				// fall back to this library's class loader
			}
		}
		return Class.forName( className );
	}

	/**
	 * Parses a comma separated list of SNMP versions: trims values, ignores case, accepts {@code 2} as an alias for
	 * {@code 2c} and drops duplicates.
	 *
	 * @throws IllegalArgumentException for an empty list or an unknown version
	 */
	static List<String> parseVersions( String value )
	{
		Set<String> versions = new LinkedHashSet<>();
		if ( value != null )
		{
			for ( String raw : value.split( "," ) )
			{
				String version = raw.trim().toLowerCase( Locale.ROOT );
				if ( version.isEmpty() )
				{
					continue;
				}
				switch ( version )
				{
					case "1":
						versions.add( VERSION_1 );
						break;
					case "2":
					case "2c":
						versions.add( VERSION_2C );
						break;
					default:
						throw new IllegalArgumentException( "Unsupported SNMP version '" + raw.trim() + "' in <SNMPVersions>, applicable values are 1, 2c" );
				}
			}
		}
		if ( versions.isEmpty() )
		{
			throw new IllegalArgumentException( "<SNMPVersions> is empty, applicable values are 1, 2c" );
		}
		return new ArrayList<>( versions );
	}

	static int parseInt( String value, String name, int defaultValue, int min, int max )
	{
		if ( isBlank( value ) )
		{
			return defaultValue;
		}
		int parsed;
		try
		{
			parsed = Integer.parseInt( value.trim() );
		}
		catch ( NumberFormatException e )
		{
			throw new IllegalArgumentException( "<" + name + "> must be an integer, was '" + value + "'" );
		}
		if ( parsed < min || parsed > max )
		{
			throw new IllegalArgumentException( "<" + name + "> must be between " + min + " and " + max + ", was " + parsed );
		}
		return parsed;
	}

	private static boolean isBlank( String s )
	{
		return s == null || s.trim().isEmpty();
	}

	private static String trimToNull( String s )
	{
		return isBlank( s ) ? null : s.trim();
	}

	// Fluent setters, for building the appender in code.

	public SNMPTrapAppenderPluginBuilder setSnmpVersions( String snmpVersions )
	{
		this.snmpVersions = snmpVersions;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setManagementHost( String managementHost )
	{
		this.managementHost = managementHost;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setManagementHostTrapListenPort( String managementHostTrapListenPort )
	{
		this.managementHostTrapListenPort = managementHostTrapListenPort;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setEnterpriseOID( String enterpriseOID )
	{
		this.enterpriseOID = enterpriseOID;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setLocalIPAddress( String localIPAddress )
	{
		this.localIPAddress = localIPAddress;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setLocalTrapSendPort( String localTrapSendPort )
	{
		this.localTrapSendPort = localTrapSendPort;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setSpecificTrapType( String specificTrapType )
	{
		this.specificTrapType = specificTrapType;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setGenericTrapType( String genericTrapType )
	{
		this.genericTrapType = genericTrapType;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setCommunityString( String communityString )
	{
		this.communityString = communityString;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setForwardStackTraceWithTrap( String forwardStackTraceWithTrap )
	{
		this.forwardStackTraceWithTrap = forwardStackTraceWithTrap;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setApplicationTrapOID( String applicationTrapOID )
	{
		this.applicationTrapOID = applicationTrapOID;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setVerbose( boolean verbose )
	{
		this.verbose = verbose;
		return this;
	}

	public SNMPTrapAppenderPluginBuilder setDispatcherClass( String dispatcherClass )
	{
		this.dispatcherClass = dispatcherClass;
		return this;
	}

	/**
	 * Provide utility methods.
	 */
	final public static class Util
	{
		private Util()
		{
		}

		/**
		 * Prints {@code msg}, prefixed by "SNMP_APPENDER: ", to the console when {@code verbose} is true
		 * ({@code <verbose>true</verbose>} in the appender configuration).
		 *
		 * @param verbose whether debug output is enabled
		 * @param msg     String to print
		 */
		public static void printToConsole( boolean verbose, String msg )
		{
			if ( verbose )
			{
				System.out.println( "SNMP_APPENDER: " + msg );
			}
		}

		/**
		 * Same as {@link #printToConsole(boolean, String)}, also printing the stack trace of {@code t}.
		 *
		 * @param verbose whether debug output is enabled
		 * @param msg     String to print
		 * @param t       Throwable to be printed
		 */
		public static void printToConsole( boolean verbose, String msg, Throwable t )
		{
			if ( verbose )
			{
				System.out.println( "SNMP_APPENDER: " + msg );
				t.printStackTrace( System.out );
			}
		}

		/**
		 * @return the stack trace lines of the event's throwable (see {@link #toStackTraceLines(Throwable)}), or
		 * {@code null} if {@code forwardStackTrace} is false or the event has no throwable.
		 */
		public static String[] stripThrowableStack( LogEvent event, boolean forwardStackTrace )
		{
			if ( !forwardStackTrace || event.getThrown() == null )
			{
				return null;
			}
			return toStackTraceLines( event.getThrown() );
		}

		/**
		 * Formats a throwable like {@link Throwable#printStackTrace()}, one array entry per line: the throwable
		 * itself (type and message), {@code "\tat ..."} frames, then each cause as {@code "Caused by: ..."} with
		 * its frames. A cause seen before is written once as {@code [CIRCULAR REFERENCE: ...]}.
		 */
		public static String[] toStackTraceLines( Throwable throwable )
		{
			List<String> lines = new ArrayList<>();
			Set<Throwable> seen = Collections.newSetFromMap( new IdentityHashMap<Throwable, Boolean>() );
			Throwable current = throwable;
			boolean first = true;
			while ( current != null )
			{
				if ( !seen.add( current ) )
				{
					lines.add( "[CIRCULAR REFERENCE: " + current + "]" );
					break;
				}
				lines.add( first ? current.toString() : "Caused by: " + current );
				for ( StackTraceElement element : current.getStackTrace() )
				{
					lines.add( "\tat " + element );
				}
				first = false;
				current = current.getCause();
			}
			return lines.toArray( new String[0] );
		}
	}

}
