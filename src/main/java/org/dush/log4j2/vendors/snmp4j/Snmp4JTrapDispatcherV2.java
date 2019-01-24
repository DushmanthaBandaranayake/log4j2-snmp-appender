package org.dush.log4j2.vendors.snmp4j;

import org.dush.log4j2.ConfigHolder;
import org.dush.log4j2.SNMPTrapAppenderPluginBuilder;
import org.dush.log4j2.intefaces.SNMPDispatchable;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.Date;

import static org.dush.log4j2.SNMPTrapAppenderPluginBuilder.Util.printToConsole;

/**
 * This class use org.snmp4j @see <a href="https://www.snmp4j.org/">https://www.snmp4j.org/</a> to send SNMP Version 1
 * Trap messages.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public class Snmp4JTrapDispatcherV2 implements SNMPDispatchable
{
	/**
	 * Holds configurations passed from log4j2.xml
	 */
	private ConfigHolder configHolder;
	private TransportMapping transport;

	public Snmp4JTrapDispatcherV2( ConfigHolder configHolder ) throws IOException
	{
		this.configHolder = configHolder;
		//Create Transport Mapping
		printToConsole( this + "org.snmp4j.TransportMapping.listen() Starting ......." );
		this.transport = new DefaultUdpTransportMapping();
		transport.listen();
		printToConsole( this + "Starting org.snmp4j.TransportMapping.listen() Started Successful" );
	}

	@Override public void dispatch( String message, String[] exceptionStackTrace ) throws Exception
	{
		Snmp snmp = null;
		try
		{
			//Create Target
			CommunityTarget comtarget = new CommunityTarget();
			comtarget.setCommunity( new OctetString( configHolder.getCommunityString() ) );
			comtarget.setVersion( SnmpConstants.version2c );
			comtarget.setAddress( new UdpAddress( configHolder.getManagementHostPort() ) );
			comtarget.setRetries( 2 );
			comtarget.setTimeout( 10000 );

			//Create PDU for V2
			PDU pdu = new PDU();

			// need to specify the system up time
			pdu.add( new VariableBinding( SnmpConstants.sysUpTime, new OctetString( new Date().toString() ) ) );
			pdu.add( new VariableBinding( SnmpConstants.snmpTrapOID, new OID( configHolder.getEnterpriseOID() ) ) );
			pdu.add( new VariableBinding( SnmpConstants.snmpTrapAddress, new IpAddress( configHolder.getLocalIPAddress() ) ) );

			// variable binding for Enterprise Specific objects, OID should be defined in MIB file
			pdu.add( new VariableBinding( new OID( configHolder.getApplicationTrapOID() ), new OctetString( message ) ) );

			//exception stack trace is added into VariableBindings
			if ( exceptionStackTrace != null )
			{
				for ( String otherProperty : exceptionStackTrace )
				{
					pdu.add( new VariableBinding( new OID( configHolder.getApplicationTrapOID() ), new OctetString( otherProperty ) ) );
				}
			}

			pdu.setType( PDU.TRAP );

			//Send the PDU
			snmp = new Snmp( transport );
			printToConsole( this + "Sending.. ->  V2c Trap to " + configHolder.getManagementHostPort() + " log message: " + message );
			snmp.send( pdu, comtarget );
			printToConsole( this + "Sending successful -> V2c Trap to " + configHolder.getManagementHostPort() + " log message: " + message );

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
					printToConsole( this + "Error in Closing SNMP Session -> V2c Trap to " + configHolder.getManagementHostPort() + " log message: " + message, e );
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
		return "type:org.dush.log4j2.vendors.snmp4j, version:2c ";
	}
}
