package org.dush.log4j2.vendors.snmp4j;

import org.dush.log4j2.ConfigHolder;
import org.dush.log4j2.interfaces.SNMPDispatchable;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;

import static org.dush.log4j2.SNMPTrapAppenderPluginBuilder.Util.printToConsole;

/**
 * Shared parts of the snmp4j dispatchers: settings validated once at build time, the trap target, the shared
 * {@link Snmp4JSession} and sending.
 */
abstract class AbstractSnmp4JTrapDispatcher implements SNMPDispatchable
{
	private static final String DEFAULT_COMMUNITY = "public";

	protected final ConfigHolder configHolder;
	protected final OID enterpriseOID;
	protected final OID applicationTrapOID;
	/** Agent address from {@code LocalIPAddress}; {@code null} if not configured. */
	protected final IpAddress localIpAddress;
	private final CommunityTarget target;
	private final Snmp4JSession session;
	private boolean released;

	AbstractSnmp4JTrapDispatcher( ConfigHolder configHolder, Snmp4JSession session, int snmpVersion )
	{
		this.configHolder = configHolder;
		this.enterpriseOID = parseOid( configHolder.getEnterpriseOID(), "EnterpriseOID" );
		this.applicationTrapOID = parseOid( configHolder.getApplicationTrapOID(), "ApplicationTrapOID" );
		this.localIpAddress = isBlank( configHolder.getLocalIPAddress() ) ? null : parseIp( configHolder.getLocalIPAddress() );

		if ( isBlank( configHolder.getManagementHost() ) || isBlank( configHolder.getMgtHostPort() ) )
		{
			throw new IllegalArgumentException( "<ManagementHost> and <ManagementHostTrapListenPort> are required" );
		}
		UdpAddress address = new UdpAddress( configHolder.getManagementHostPort() );
		if ( address.getInetAddress() == null )
		{
			throw new IllegalArgumentException( "Invalid management host/port: " + configHolder.getManagementHostPort() );
		}

		target = new CommunityTarget();
		target.setCommunity( new OctetString( isBlank( configHolder.getCommunityString() ) ? DEFAULT_COMMUNITY : configHolder.getCommunityString() ) );
		target.setVersion( snmpVersion );
		target.setAddress( address );
		target.setRetries( 2 );
		target.setTimeout( 10000 );

		this.session = session.acquire();
	}

	/**
	 * Builds the trap PDU for one log message.
	 */
	abstract PDU buildPdu( String message, String[] exceptionStackTrace );

	/**
	 * Adds the log message varbind and, if present, one varbind per stack trace line, all under ApplicationTrapOID.
	 */
	protected void addMessageBindings( PDU pdu, String message, String[] exceptionStackTrace )
	{
		pdu.add( new VariableBinding( applicationTrapOID, new OctetString( message == null ? "" : message ) ) );
		if ( exceptionStackTrace != null )
		{
			for ( String line : exceptionStackTrace )
			{
				pdu.add( new VariableBinding( applicationTrapOID, new OctetString( line ) ) );
			}
		}
	}

	@Override public void dispatch( String message, String[] exceptionStackTrace ) throws Exception
	{
		PDU pdu = buildPdu( message, exceptionStackTrace );
		boolean verbose = configHolder.isVerbose();
		printToConsole( verbose, this + "Sending.. -> trap to " + configHolder.getManagementHostPort() + " log message: " + message );
		session.getSnmp().send( pdu, target );
		printToConsole( verbose, this + "Sending successful -> trap to " + configHolder.getManagementHostPort() + " log message: " + message );
	}

	@Override public synchronized void releaseResources() throws Exception
	{
		if ( !released )
		{
			released = true;
			session.release();
		}
	}

	CommunityTarget getTarget()
	{
		return target;
	}

	private static OID parseOid( String value, String name )
	{
		if ( isBlank( value ) )
		{
			throw new IllegalArgumentException( "<" + name + "> is required" );
		}
		OID oid = new OID( value.trim() );
		if ( !oid.isValid() )
		{
			throw new IllegalArgumentException( "Invalid OID for <" + name + ">: " + value );
		}
		return oid;
	}

	private static IpAddress parseIp( String value )
	{
		try
		{
			return new IpAddress( value.trim() );
		}
		catch ( RuntimeException e )
		{
			throw new IllegalArgumentException( "Invalid <LocalIPAddress>: " + value, e );
		}
	}

	private static boolean isBlank( String s )
	{
		return s == null || s.trim().isEmpty();
	}
}
