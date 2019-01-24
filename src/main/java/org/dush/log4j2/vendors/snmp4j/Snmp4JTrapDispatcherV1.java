package org.dush.log4j2.vendors.snmp4j;

import org.dush.log4j2.ConfigHolder;
import org.dush.log4j2.SNMPTrapAppenderPluginBuilder;
import org.dush.log4j2.intefaces.SNMPDispatchable;
import org.snmp4j.PDUv1;
import org.snmp4j.TransportMapping;

import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import static org.dush.log4j2.SNMPTrapAppenderPluginBuilder.Util.printToConsole;

/**
 * This class use <b>org.snmp4j</b> (<a href="https://www.snmp4j.org/">https://www.snmp4j.org/</a>) to send SNMP Version 2c
 * Trap messages.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public class Snmp4JTrapDispatcherV1 implements SNMPDispatchable
{
	/**
	 * Holds configurations passed from log4j2.xml
	 */
	private ConfigHolder configHolder;
	private TransportMapping transport;

	public Snmp4JTrapDispatcherV1( ConfigHolder configHolder ) throws IOException
	{
		this.configHolder = configHolder;
		this.transport = new DefaultUdpTransportMapping();
		transport.listen();
	}

	public void dispatch( String message, String[] exceptionStackTrace ) throws Exception
	{
		Snmp snmp = null;
		try
		{
			//Create Target
			CommunityTarget comTarget = new CommunityTarget();
			comTarget.setCommunity( new OctetString( configHolder.getCommunityString() ) );
			comTarget.setVersion( SnmpConstants.version1 );
			comTarget.setAddress( new UdpAddress( configHolder.getManagementHostPort() ) );
			comTarget.setRetries( 2 );
			comTarget.setTimeout( 10000 );

			//Create PDU for V1
			PDUv1 pdu = new PDUv1();
			pdu.setType( PDU.V1TRAP );
			pdu.setEnterprise( new OID( configHolder.getEnterpriseOID() ) );
			pdu.setGenericTrap( Integer.parseInt( configHolder.getGenericTrapType() ) );
			pdu.setSpecificTrap( configHolder.isSpecific() ? 1 : 0 );
			pdu.setAgentAddress( new IpAddress( configHolder.getLocalIPAddress() ) );

			//varbind, with the applicationTrapOID as the name, and the logging event string as the value...
			pdu.add( new VariableBinding( new OID( configHolder.getApplicationTrapOID() ), new OctetString( message ) ) );

			//exception stack trace is added into VariableBindings
			if ( exceptionStackTrace != null )
			{
				for ( String otherProperty : exceptionStackTrace )
				{
					pdu.add( new VariableBinding( new OID( configHolder.getApplicationTrapOID() ), new OctetString( otherProperty ) ) );
				}
			}

			//Send the PDU
			snmp = new Snmp( transport );
			printToConsole( this + "Sending.. ->  V1 Trap to " + configHolder.getManagementHostPort() + " log message: " + message );
			snmp.send( pdu, comTarget );
			printToConsole( this + "Sending successful -> V1 Trap to " + configHolder.getManagementHostPort() + " log message: " + message );

		}

		finally
		{
			if ( snmp != null )
			{
				try
				{
					snmp.close();
				}
				catch ( Exception e )
				{
					printToConsole( this + "Error in Closing SNMP Session -> V1 Trap to " + configHolder.getManagementHostPort() + " log message: " + message, e );
				}
			}

		}
	}

	@Override public void releaseResources() throws Exception
	{
		if ( transport != null )
		{
			transport.close();
		}
	}
	@Override public String toString()
	{
		return "type:org.dush.log4j2.vendors.snmp4j, version:1 ";
	}
}
