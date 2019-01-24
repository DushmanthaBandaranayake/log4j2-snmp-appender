package org.dush.log4j2.vendors.snmp4j;

import org.dush.log4j2.ConfigHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class Snmp4JTrapDispatcherV1Test
{

	private static Snmp4JTrapDispatcherV1 snmp4JTrapDispatcherV1;

	@BeforeAll
	static void init()
	{
		try
		{
			snmp4JTrapDispatcherV1 = new Snmp4JTrapDispatcherV1( new ConfigHolder() );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	@Test void dispatchTest()
	{
		Assertions.assertThrows( NullPointerException.class,
				() -> {
					snmp4JTrapDispatcherV1.dispatch( null, null );
				} );

	}

}
