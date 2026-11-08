package org.dush.log4j2;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.dush.log4j2.interfaces.SNMPDispatchable;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

//@formatter:off
/**
 * Sample configuration
 * {@code
 * 			<SNMPTrapAppender name="SNMPTrapAppender" >
 * 				<PatternLayout pattern="%m"/>
 * 				<verbose>true</verbose>
 * 				<SNMPVersions>1</SNMPVersions>
 *				<ManagementHost>127.0.0.1</ManagementHost>
 * 				<ManagementHostTrapListenPort>162</ManagementHostTrapListenPort>
 * 				<EnterpriseOID>1.3.6.1.4.1.24.0</EnterpriseOID>
 * 				<LocalIPAddress>127.0.0.1</LocalIPAddress>
 * 				<LocalTrapSendPort>0</LocalTrapSendPort>
 * 				<GenericTrapType>6</GenericTrapType>
 * 				<SpecificTrapType>12345678</SpecificTrapType>
 * 				<CommunityString>public</CommunityString>
 *				<ForwardStackTraceWithTrap>true</ForwardStackTraceWithTrap>
 * 				<ApplicationTrapOID>1.3.6.1.4.1.24.12.10.22.64</ApplicationTrapOID>
 * 		    </SNMPTrapAppender>
 * }
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
//@formatter:on
@Plugin(name = "SNMPTrapAppender", category = "Core", elementType = "appender")
public final class SNMPTrapAppender extends AbstractAppender
{
	private final List<SNMPDispatchable> snmpDispatchables;
	private final boolean forwardStackTrace;

	@PluginBuilderFactory
	public static SNMPTrapAppenderPluginBuilder newBuilder()
	{
		return new SNMPTrapAppenderPluginBuilder();
	}

	SNMPTrapAppender( String name,
			Filter filter,
			Layout<? extends Serializable> layout,
			boolean ignoreExceptions,
			Property[] properties,
			List<SNMPDispatchable> snmpDispatchables,
			ConfigHolder configHolder )
	{
		super( name, filter, layout, ignoreExceptions, properties );
		this.snmpDispatchables = Collections.unmodifiableList( snmpDispatchables );
		this.forwardStackTrace = configHolder.isForwardStackTraceWithTrap();
	}

	@Override
	public void append( LogEvent event )
	{
		String message = formatMsg( event );
		String[] stackTrace = SNMPTrapAppenderPluginBuilder.Util.stripThrowableStack( event, forwardStackTrace );
		Exception failure = null;
		for ( SNMPDispatchable snmpDispatchable : snmpDispatchables )
		{
			try
			{
				snmpDispatchable.dispatch( message, stackTrace );
			}
			catch ( final Exception e )
			{
				failure = e;
				error( "Unable to send SNMP message in appender [" + getName() + "], for dispatcher [" + snmpDispatchable + "] ", event, e );
			}
		}
		if ( failure != null && !ignoreExceptions() )
		{
			throw new AppenderLoggingException( "Unable to send SNMP message in appender [" + getName() + "]", failure );
		}
	}

	@Override
	public boolean stop( long timeout, TimeUnit timeUnit )
	{
		setStopping();
		boolean stopped = super.stop( timeout, timeUnit, false );
		for ( SNMPDispatchable snmpDispatchable : snmpDispatchables )
		{
			try
			{
				snmpDispatchable.releaseResources();
			}
			catch ( Exception e )
			{
				LOGGER.error( "Unable to release resources of dispatcher [{}] in appender [{}]", snmpDispatchable, getName(), e );
				stopped = false;
			}
		}
		setStopped();
		return stopped;
	}

	List<SNMPDispatchable> getDispatchers()
	{
		return snmpDispatchables;
	}

	/**
	 * Formats the log message with the configured layout. String layouts are used as is; other layouts are
	 * serialised to bytes and decoded as UTF-8.
	 */
	private String formatMsg( LogEvent event )
	{
		Layout<? extends Serializable> layout = getLayout();
		Serializable serializable = layout.toSerializable( event );
		if ( serializable instanceof String )
		{
			return ( String ) serializable;
		}
		return new String( layout.toByteArray( event ), StandardCharsets.UTF_8 );
	}

}
