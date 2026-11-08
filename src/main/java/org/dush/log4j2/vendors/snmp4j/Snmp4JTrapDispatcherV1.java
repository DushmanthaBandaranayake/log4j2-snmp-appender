package org.dush.log4j2.vendors.snmp4j;

import org.dush.log4j2.ConfigHolder;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;

/**
 * Uses <b>org.snmp4j</b> (<a href="https://www.snmp4j.org/">https://www.snmp4j.org/</a>) to send SNMP version 1
 * trap messages.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public class Snmp4JTrapDispatcherV1 extends AbstractSnmp4JTrapDispatcher
{
	private static final IpAddress ANY_ADDRESS = new IpAddress( "0.0.0.0" );

	public Snmp4JTrapDispatcherV1( ConfigHolder configHolder, Snmp4JSession session )
	{
		super( configHolder, session, SnmpConstants.version1 );
	}

	@Override PDUv1 buildPdu( String message, String[] exceptionStackTrace )
	{
		PDUv1 pdu = new PDUv1();
		pdu.setType( PDU.V1TRAP );
		pdu.setEnterprise( enterpriseOID );
		pdu.setGenericTrap( configHolder.getGenericTrapType() );
		pdu.setSpecificTrap( configHolder.getSpecificTrapType() );
		pdu.setAgentAddress( localIpAddress != null ? localIpAddress : ANY_ADDRESS );
		pdu.setTimestamp( Snmp4JSession.sysUpTimeTicks() );

		// varbind, with the applicationTrapOID as the name, and the logging event string as the value
		addMessageBindings( pdu, message, exceptionStackTrace );
		return pdu;
	}

	@Override public String toString()
	{
		return "type:org.dush.log4j2.vendors.snmp4j, version:1 ";
	}
}
