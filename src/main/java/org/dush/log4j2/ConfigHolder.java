package org.dush.log4j2;

/**
 * Simply wraps SNMPTrapAppenderPluginBuilder. Without directly using {@link SNMPTrapAppenderPluginBuilder}(as it could be heavy), object of
 * this calss can be used to transfer configurations.
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
	private String localTrapSendPort;
	private String genericTrapType;
	private boolean isSpecific;
	private String communityString;
	private String forwardStackTraceWithTrap;
	private String applicationTrapOID;
	private boolean verbose = false;

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

	public String getLocalTrapSendPort()
	{
		return localTrapSendPort;
	}

	public void setLocalTrapSendPort( String localTrapSendPort )
	{
		this.localTrapSendPort = localTrapSendPort;
	}

	public String getGenericTrapType()
	{
		return genericTrapType;
	}

	public void setGenericTrapType( String genericTrapType )
	{
		this.genericTrapType = genericTrapType;
	}

	public boolean isSpecific()
	{
		return isSpecific;
	}

	public void setSpecific( boolean specific )
	{
		this.isSpecific = specific;
	}

	public String getCommunityString()
	{
		return communityString;
	}

	public void setCommunityString( String communityString )
	{
		this.communityString = communityString;
	}

	public String getForwardStackTraceWithTrap()
	{
		return forwardStackTraceWithTrap;
	}

	public void setForwardStackTraceWithTrap( String forwardStackTraceWithTrap )
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

	public String getManagementHostPort()
	{
		return ( managementHost + "/" + managementHostTrapListenPort ).trim();
	}
}
