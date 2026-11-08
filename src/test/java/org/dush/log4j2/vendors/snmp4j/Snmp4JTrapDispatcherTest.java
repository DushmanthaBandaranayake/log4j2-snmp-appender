package org.dush.log4j2.vendors.snmp4j;

import org.dush.log4j2.ConfigHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Snmp4JTrapDispatcherTest
{
	private static final OID APP_OID = new OID( "1.3.6.1.4.1.24.12.10.22.64" );
	private static final OID ENTERPRISE_OID = new OID( "1.3.6.1.4.1.24.0" );

	private Snmp4JSession session;

	@BeforeEach void openSession() throws IOException
	{
		session = new Snmp4JSession( 0 );
	}

	@AfterEach void closeSession() throws IOException
	{
		session.release();
	}

	@Test void v1PduCarriesConfiguredTrapFields()
	{
		ConfigHolder config = config();
		config.setGenericTrapType( 6 );
		config.setSpecificTrapType( 123 );
		Snmp4JTrapDispatcherV1 dispatcher = new Snmp4JTrapDispatcherV1( config, session );

		PDUv1 pdu = dispatcher.buildPdu( "hello", new String[] { "java.lang.Exception: x", "\tat a.B.c(B.java:1)" } );

		assertEquals( PDU.V1TRAP, pdu.getType() );
		assertEquals( ENTERPRISE_OID, pdu.getEnterprise() );
		assertEquals( 6, pdu.getGenericTrap() );
		assertEquals( 123, pdu.getSpecificTrap() );
		assertEquals( new IpAddress( "127.0.0.1" ), pdu.getAgentAddress() );
		assertTrue( pdu.getTimestamp() >= 0 );
		assertEquals( 3, pdu.size() );
		assertEquals( APP_OID, pdu.get( 0 ).getOid() );
		assertEquals( new OctetString( "hello" ), pdu.get( 0 ).getVariable() );
		assertEquals( new OctetString( "\tat a.B.c(B.java:1)" ), pdu.get( 2 ).getVariable() );
		assertEquals( SnmpConstants.version1, dispatcher.getTarget().getVersion() );
	}

	@Test void v2PduStartsWithTimeTicksUptimeAndTrapOid()
	{
		Snmp4JTrapDispatcherV2 dispatcher = new Snmp4JTrapDispatcherV2( config(), session );

		PDU pdu = dispatcher.buildPdu( "hello", null );

		assertEquals( PDU.TRAP, pdu.getType() );
		assertEquals( SnmpConstants.sysUpTime, pdu.get( 0 ).getOid() );
		assertInstanceOf( TimeTicks.class, pdu.get( 0 ).getVariable() );
		assertEquals( SnmpConstants.snmpTrapOID, pdu.get( 1 ).getOid() );
		assertEquals( ENTERPRISE_OID, pdu.get( 1 ).getVariable() );
		assertEquals( SnmpConstants.snmpTrapAddress, pdu.get( 2 ).getOid() );
		assertEquals( new OctetString( "hello" ), pdu.get( 3 ).getVariable() );
		assertEquals( 4, pdu.size() );
		assertEquals( SnmpConstants.version2c, dispatcher.getTarget().getVersion() );
	}

	@Test void v2PduOmitsTrapAddressWhenNotConfigured()
	{
		ConfigHolder config = config();
		config.setLocalIPAddress( null );

		PDU pdu = new Snmp4JTrapDispatcherV2( config, session ).buildPdu( "hello", null );

		assertEquals( 3, pdu.size() );
		assertEquals( APP_OID, pdu.get( 2 ).getOid() );
	}

	@Test void communityDefaultsToPublic()
	{
		ConfigHolder config = config();
		config.setCommunityString( null );

		assertEquals( new OctetString( "public" ), new Snmp4JTrapDispatcherV2( config, session ).getTarget().getCommunity() );
	}

	@Test void invalidSettingsFailAtConstruction()
	{
		ConfigHolder noOid = config();
		noOid.setApplicationTrapOID( null );
		assertThrows( IllegalArgumentException.class, () -> new Snmp4JTrapDispatcherV1( noOid, session ) );

		ConfigHolder badOid = config();
		badOid.setEnterpriseOID( "not.an.oid" );
		assertThrows( RuntimeException.class, () -> new Snmp4JTrapDispatcherV1( badOid, session ) );

		ConfigHolder noHost = config();
		noHost.setManagementHost( null );
		assertThrows( IllegalArgumentException.class, () -> new Snmp4JTrapDispatcherV2( noHost, session ) );
	}

	@Test void sessionClosesWhenLastReferenceIsReleased() throws Exception
	{
		Snmp4JSession shared = new Snmp4JSession( 0 );
		Snmp4JTrapDispatcherV1 v1 = new Snmp4JTrapDispatcherV1( config(), shared );
		Snmp4JTrapDispatcherV2 v2 = new Snmp4JTrapDispatcherV2( config(), shared );
		shared.release(); // creator's reference

		v1.releaseResources();
		v1.releaseResources(); // second release of the same dispatcher must not drop v2's reference
		assertFalse( shared.isClosed() );

		v2.releaseResources();
		assertTrue( shared.isClosed() );
		assertThrows( IllegalStateException.class, shared::acquire );
	}

	private static ConfigHolder config()
	{
		ConfigHolder config = new ConfigHolder();
		config.setManagementHost( "127.0.0.1" );
		config.setMgtHostPort( "162" );
		config.setEnterpriseOID( ENTERPRISE_OID.toDottedString() );
		config.setApplicationTrapOID( APP_OID.toDottedString() );
		config.setLocalIPAddress( "127.0.0.1" );
		config.setCommunityString( "public" );
		return config;
	}
}
