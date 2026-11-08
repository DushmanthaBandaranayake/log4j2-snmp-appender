package org.dush.log4j2;

import org.dush.log4j2.SNMPTrapAppenderPluginBuilder.Util;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilTest
{
	@Test void noStackTraceWhenDisabledOrNoThrowable()
	{
		assertNull( Util.stripThrowableStack( SNMPTrapAppenderPluginBuilderTest.event( "x", new RuntimeException() ), false ) );
		assertNull( Util.stripThrowableStack( SNMPTrapAppenderPluginBuilderTest.event( "x", null ), true ) );
	}

	@Test void includesTypeMessageFramesAndCauses()
	{
		IllegalStateException root = new IllegalStateException( "root cause" );
		RuntimeException top = new RuntimeException( "top", root );

		List<String> lines = Arrays.asList( Util.stripThrowableStack( SNMPTrapAppenderPluginBuilderTest.event( "x", top ), true ) );

		assertEquals( "java.lang.RuntimeException: top", lines.get( 0 ) );
		assertEquals( "\tat " + top.getStackTrace()[0], lines.get( 1 ) );
		int causeIndex = 1 + top.getStackTrace().length;
		assertEquals( "Caused by: java.lang.IllegalStateException: root cause", lines.get( causeIndex ) );
		assertEquals( causeIndex + 1 + root.getStackTrace().length, lines.size() );
	}

	@Test void cyclicCauseDoesNotLoop()
	{
		Exception a = new Exception( "a" );
		Exception b = new Exception( "b", a );
		a.initCause( b );

		String[] lines = Util.toStackTraceLines( a );

		assertTrue( lines[lines.length - 1].startsWith( "[CIRCULAR REFERENCE: java.lang.Exception: a" ) );
	}
}
