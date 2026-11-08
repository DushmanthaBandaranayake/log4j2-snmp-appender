package org.dush.log4j2.vendors.snmp4j;

import org.snmp4j.Snmp;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;

/**
 * One UDP transport and one {@link Snmp} instance, shared by all snmp4j dispatchers of an appender.
 * <p>
 * Sharing is needed because the v1 and v2c dispatchers would otherwise both try to bind the configured
 * {@code LocalTrapSendPort}. The session is reference counted: the creator holds one reference and each dispatcher
 * {@link #acquire() acquires} another. The socket is closed when the last reference is {@link #release() released}.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 */
public final class Snmp4JSession
{
	private final DefaultUdpTransportMapping transport;
	private final Snmp snmp;
	private int references = 1;
	private boolean closed;

	/**
	 * Opens the UDP socket.
	 *
	 * @param localPort local port to send from; {@code 0} or less binds an ephemeral port.
	 * @throws IOException when the socket cannot be bound, e.g. the port is in use or needs privileges.
	 */
	public Snmp4JSession( int localPort ) throws IOException
	{
		transport = localPort > 0
				? new DefaultUdpTransportMapping( new UdpAddress( InetAddress.getByName( "0.0.0.0" ), localPort ) )
				: new DefaultUdpTransportMapping();
		snmp = new Snmp( transport );
		snmp.listen();
	}

	/**
	 * Adds a reference to this session.
	 *
	 * @return this session
	 * @throws IllegalStateException when the session is already closed
	 */
	public synchronized Snmp4JSession acquire()
	{
		if ( closed )
		{
			throw new IllegalStateException( "SNMP session is already closed" );
		}
		references++;
		return this;
	}

	/**
	 * Drops a reference; closes the socket when none are left. Calling it on a closed session does nothing.
	 *
	 * @throws IOException when closing the socket fails
	 */
	public synchronized void release() throws IOException
	{
		if ( closed )
		{
			return;
		}
		if ( --references <= 0 )
		{
			closed = true;
			snmp.close();
		}
	}

	public Snmp getSnmp()
	{
		return snmp;
	}

	public synchronized boolean isClosed()
	{
		return closed;
	}

	/**
	 * @return the local address the socket is bound to.
	 */
	public UdpAddress getLocalAddress()
	{
		return transport.getAddress();
	}

	/**
	 * @return JVM uptime in hundredths of a second, as used by sysUpTime and the v1 trap timestamp.
	 */
	static long sysUpTimeTicks()
	{
		return ManagementFactory.getRuntimeMXBean().getUptime() / 10;
	}
}
