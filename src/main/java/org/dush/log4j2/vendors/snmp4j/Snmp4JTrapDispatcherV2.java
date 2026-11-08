package org.dush.log4j2.vendors.snmp4j;

import org.dush.log4j2.ConfigHolder;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;

/**
 * Uses <b>org.snmp4j</b> (<a href="https://www.snmp4j.org/">https://www.snmp4j.org/</a>) to send SNMP version 2c
 * trap messages.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public class Snmp4JTrapDispatcherV2 extends AbstractSnmp4JTrapDispatcher
{
	public Snmp4JTrapDispatcherV2( ConfigHolder configHolder, Snmp4JSession session )
	{
		super( configHolder, session, SnmpConstants.version2c );
	}

	@Override PDU buildPdu( String message, String[] exceptionStackTrace )
	{
		PDU pdu = new PDU();
		pdu.setType( PDU.TRAP );

		// RFC 3416: the first two varbinds of a v2 trap are sysUpTime.0 (TimeTicks) and snmpTrapOID.0
		pdu.add( new VariableBinding( SnmpConstants.sysUpTime, new TimeTicks( Snmp4JSession.sysUpTimeTicks() ) ) );
		pdu.add( new VariableBinding( SnmpConstants.snmpTrapOID, enterpriseOID ) );
		if ( localIpAddress != null )
		{
			pdu.add( new VariableBinding( SnmpConstants.snmpTrapAddress, localIpAddress ) );
		}

		// variable binding for Enterprise Specific objects, OID should be defined in MIB file
		addMessageBindings( pdu, message, exceptionStackTrace );
		return pdu;
	}

	@Override public String toString()
	{
		return "type:org.dush.log4j2.vendors.snmp4j, version:2c ";
	}
}
