package org.dush.log4j2;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.dush.log4j2.intefaces.SNMPDispatchable;

import java.io.Serializable;
import java.util.List;

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
 * 				<LocalTrapSendPort>161</LocalTrapSendPort>
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
//@formatter:off
@Plugin(name = "SNMPTrapAppender", category = "Core", elementType = "appender")
public final class SNMPTrapAppender extends AbstractAppender
{
	private final List<SNMPDispatchable> snmpDispatchables ;

	@PluginBuilderFactory
	public static org.apache.logging.log4j.core.util.Builder<SNMPTrapAppender> newBuilder()
	{
		return new SNMPTrapAppenderPluginBuilder();
	}

	protected SNMPTrapAppender( String name,
			Filter filter,
			Layout<? extends Serializable> layout,
			List<SNMPDispatchable> snmpDispatchables )
	{
		super( name, filter, layout );
		this.snmpDispatchables = snmpDispatchables;

		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			public void run()
			{
				for ( SNMPDispatchable snmpDispatchable : snmpDispatchables )
				{
					try
					{
						snmpDispatchable.releaseResources();
					}
					catch ( Exception e )
					{
						e.printStackTrace();
					}
				}
			}
		} );
	}

	public void append( LogEvent event )
	{
		for ( SNMPDispatchable snmpDispatchable : snmpDispatchables )
		{
			try
			{
				snmpDispatchable.dispatch( formatMsg( event ), SNMPTrapAppenderPluginBuilder.Util.stripThrowableStack( event ) );
			}
			catch ( final Exception e )
			{
				error( "Unable to send SNMP message in appender [" + getName() + "], for dispatcher [" + snmpDispatchable + "] ", event, e );
			}
		}
	}

	/**
	 * make log message according to the specified pattern layout
	 *
	 * @param event
	 * @return
	 */
	private String formatMsg( LogEvent event )
	{
		return ( String ) getLayout().toSerializable( event );
	}

}
