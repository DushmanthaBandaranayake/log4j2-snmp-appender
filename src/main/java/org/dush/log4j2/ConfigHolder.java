package org.dush.log4j2;

import java.util.Collections;
import java.util.List;

/**
 * Holds the parsed appender configuration and passes it to the dispatchers, so they don't depend on
 * {@link SNMPTrapAppenderPluginBuilder} directly.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public class ConfigHolder
{
	private String managementHost;
	private String managementHostTrapListenPort;
	private String enterpriseOID;
	private String localIPAddress;
	private int localTrapSendPort;
	private int genericTrapType;
	private int specificTrapType;
	private String communityString;
	private boolean forwardStackTraceWithTrap;
	private String applicationTrapOID;
	private boolean verbose = false;
	private List<String> snmpVersions = Collections.emptyList();

	public String getManagementHost()
	{
		return managementHost;
	}

	public void setManagementHost( String managementHost )
	{
		this.managementHost = managementHost;
	}

	public String getMgtHostPort()
	{
		return managementHostTrapListenPort;
	}

	public void setMgtHostPort( String managementHostTrapListenPort )
	{
		this.managementHostTrapListenPort = managementHostTrapListenPort;
	}

	public String getEnterpriseOID()
	{
		return enterpriseOID;
	}

	public void setEnterpriseOID( String enterpriseOID )
	{
		this.enterpriseOID = enterpriseOID;
	}

	public String getLocalIPAddress()
	{
		return localIPAddress;
	}

	public void setLocalIPAddress( String localIPAddress )
	{
		this.localIPAddress = localIPAddress;
	}

	/**
	 * @return local UDP port traps are sent from; {@code 0} means an ephemeral port chosen by the OS.
	 */
	public int getLocalTrapSendPort()
	{
		return localTrapSendPort;
	}

	public void setLocalTrapSendPort( int localTrapSendPort )
	{
		this.localTrapSendPort = localTrapSendPort;
	}

	public int getGenericTrapType()
	{
		return genericTrapType;
	}

	public void setGenericTrapType( int genericTrapType )
	{
		this.genericTrapType = genericTrapType;
	}

	/**
	 * @return specific trap code sent in SNMP v1 traps.
	 */
	public int getSpecificTrapType()
	{
		return specificTrapType;
	}

	public void setSpecificTrapType( int specificTrapType )
	{
		this.specificTrapType = specificTrapType;
	}

	public String getCommunityString()
	{
		return communityString;
	}

	public void setCommunityString( String communityString )
	{
		this.communityString = communityString;
	}

	public boolean isForwardStackTraceWithTrap()
	{
		return forwardStackTraceWithTrap;
	}

	public void setForwardStackTraceWithTrap( boolean forwardStackTraceWithTrap )
	{
		this.forwardStackTraceWithTrap = forwardStackTraceWithTrap;
	}

	public String getApplicationTrapOID()
	{
		return applicationTrapOID;
	}

	public void setApplicationTrapOID( String applicationTrapOID )
	{
		this.applicationTrapOID = applicationTrapOID;
	}

	public boolean isVerbose()
	{
		return verbose;
	}

	public void setVerbose( boolean verbose )
	{
		this.verbose = verbose;
	}

	/**
	 * @return the configured SNMP versions, normalised to {@code "1"} and {@code "2c"}, without duplicates.
	 */
	public List<String> getSnmpVersions()
	{
		return snmpVersions;
	}

	public void setSnmpVersions( List<String> snmpVersions )
	{
		this.snmpVersions = snmpVersions == null ? Collections.<String>emptyList() : Collections.unmodifiableList( snmpVersions );
	}

	/**
	 * @return management host and port in snmp4j address form, e.g. {@code 127.0.0.1/162}.
	 */
	public String getManagementHostPort()
	{
		return ( managementHost + "/" + managementHostTrapListenPort ).trim();
	}
}
