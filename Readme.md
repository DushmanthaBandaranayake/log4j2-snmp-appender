# log4j2-snmp-appender

Log4j2 appender that sends log events as SNMP traps (v1 and/or v2c). It uses [SNMP4J](https://www.snmp4j.org/) by default, and you can plug in your own implementation (see [Using a custom implementation](#using-a-custom-implementation)).

## Requirements
- Java 8 or later
- Log4j 2.x (built and tested against 2.25.5)

The appender brings these dependencies:
```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.25.5</version>
</dependency>
<dependency>
    <groupId>org.snmp4j</groupId>
    <artifactId>snmp4j</artifactId>
    <version>2.8.18</version>
</dependency>
```

## Usage
Sample `log4j2.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss,SSS} [%t] %-5p %c{36} - %m%n"/>
        </Console>

        <!--Setting up SNMPTrapAppender -->
        <SNMPTrapAppender name="SNMPTrapAppender" >

            <!--This pattern will be sent in a varbind.
            For example "SNMPv2-SMI::enterprises.24.12.10.22.64 = 13:59:30,017 [main] INFO TEST - info!"
            alwaysWriteExceptions="false" stops the layout appending the stack trace to the message;
            it is sent in separate varbinds when ForwardStackTraceWithTrap is true.-->
            <PatternLayout pattern="%d{HH:mm:ss,SSS} [%t] %-5p %c{36} - %m" alwaysWriteExceptions="false"/>

            <!--if true prints debugging information on console(system.print())-->
            <verbose>true</verbose>

            <!--Comma separated SNMP versions. Applicable values are 1, 2c (or 2)-->
            <SNMPVersions>2c,1</SNMPVersions>

           <!-- IP address of the remote host that traps sent in to.-->
            <ManagementHost>127.0.0.1</ManagementHost>

            <!--listening port for SNMP traps in host. could be any TCP/IP port. The standard is 162-->
            <ManagementHostTrapListenPort>162</ManagementHostTrapListenPort>

            <!--Enterprise OID that will be sent in the SNMP PDU.
            Ex: 1.3.6.1.2.1.1.2.0 points to the standard sysObjectID of the "systemName" node of the standard system MIB.-->
            <EnterpriseOID>1.3.6.1.4.1.24.0</EnterpriseOID>

            <!--IP address of the host that is using this appender to send SNMP traps. This address will be encoded in the SNMP PDU-->
            <LocalIPAddress>127.0.0.1</LocalIPAddress>

            <!--Local UDP port traps are sent from. 0 or empty picks a free port.
            Ports below 1024 (e.g. 161) usually need root privileges.-->
            <LocalTrapSendPort>0</LocalTrapSendPort>

            <!-- GenericTrapType int, Applicable for SNMP v1

                Value	Type
                0	    coldStart
                1	    warmStart
                2	    linkDown
                3	    linkUp
                4	    authenticationFailure
                5	    egpNeighborLoss
                6	    enterpriseSpecific
            -->
            <GenericTrapType>6</GenericTrapType>

            <!-- Specific trap code, applicable for SNMP v1. An INTEGER (Integer32), defaults to 0-->
            <SpecificTrapType>123</SpecificTrapType>

            <!--community string for the SNMP session. Defaults to "public".-->
            <CommunityString>public</CommunityString>

            <!--Add each line of exception stack trace in to separate varbindings. applicable values 'true','false' -->
            <ForwardStackTraceWithTrap>true</ForwardStackTraceWithTrap>

            <!-- OID that will be sent in the SNMP PDU for this app.
                 variable binding for Enterprise Specific objects, this should be available in MIB -->
            <ApplicationTrapOID>1.3.6.1.4.1.24.12.10.22.64</ApplicationTrapOID>

            <!-- Other than above settings, for SNMP version 2c, "retries= 2" and "timeout=10sec"-->
        </SNMPTrapAppender>
        <!--End of Setting up SNMPTrapAppender -->
    </Appenders>

    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="ConsoleAppender"/>
        </Root>
        <Logger name="TEST" level="ALL">
            <!--applying SNMPTrapAppender to the logger named "TEST"-->
            <AppenderRef ref="SNMPTrapAppender"/>
        </Logger>
    </Loggers>
</Configuration>
```

### Settings

| Element | Required | Description |
|---|---|---|
| `SNMPVersions` | yes (built-in dispatchers) | Comma separated: `1`, `2c` (`2` is accepted as an alias). One trap is sent per version for each log event. Unknown values are a configuration error. |
| `ManagementHost` | yes | Host the traps are sent to. |
| `ManagementHostTrapListenPort` | yes | Trap port on the management host, usually `162`. |
| `EnterpriseOID` | yes | v1: enterprise field. v2c: value of `snmpTrapOID.0`. |
| `ApplicationTrapOID` | yes | OID of the varbinds that carry the log message and stack trace lines. |
| `LocalIPAddress` | no | v1: agent address (`0.0.0.0` if not set). v2c: sent as `snmpTrapAddress.0` if set. |
| `LocalTrapSendPort` | no | Local UDP port traps are sent from. Empty or `0` picks a free port. Ports below 1024 (e.g. `161`) usually need root. |
| `GenericTrapType` | no | v1 generic trap, `0`–`6`. Defaults to `6` (enterpriseSpecific). |
| `SpecificTrapType` | no | v1 specific trap code, an Integer32. Defaults to `0`. |
| `CommunityString` | no | Defaults to `public`. |
| `ForwardStackTraceWithTrap` | no | `true` adds the exception's stack trace (type and message, frames, causes) as one varbind per line. |
| `verbose` | no | `true` prints debug output to `System.out`. |
| `DispatcherClass` | no | Custom dispatcher class names, see below. |
| `ignoreExceptions` | no | Standard Log4j appender attribute. When `false`, a failed send is rethrown to the caller. |

v2c traps start with `sysUpTime.0` (JVM uptime as TimeTicks) and `snmpTrapOID.0`, as RFC 3416 requires. v1 traps use the same uptime as their timestamp.

Invalid settings are reported by the Log4j status logger when the configuration loads, and the appender is not created.

By default `PatternLayout` also appends the exception's stack trace to the message. When `ForwardStackTraceWithTrap` is `true`, set `alwaysWriteExceptions="false"` on the layout so the trace isn't sent twice.

The appender's socket is closed when Log4j stops or reloads the configuration.

### Using a custom implementation
To send traps with another SNMP library, implement `org.dush.log4j2.interfaces.SNMPDispatchable`:

```java
package com.example;

import org.dush.log4j2.ConfigHolder;
import org.dush.log4j2.interfaces.SNMPDispatchable;

public class MyDispatcher implements SNMPDispatchable
{
	private final ConfigHolder config;

	// A (ConfigHolder) constructor is used if present, otherwise a public no-arg constructor.
	public MyDispatcher( ConfigHolder config )
	{
		this.config = config;   // parsed settings, including config.getSnmpVersions()
	}

	@Override public void dispatch( String message, String[] exceptionStackTrace ) throws Exception
	{
		// message: the event formatted by the layout
		// exceptionStackTrace: one entry per line, or null
	}

	@Override public void releaseResources() throws Exception
	{
		// called when the appender stops
	}
}
```

Then name it in the configuration (comma separate several classes). The class and its libraries must be on the classpath. When `DispatcherClass` is set, the built-in SNMP4J dispatchers are not created:

```xml
<SNMPTrapAppender name="SNMPTrapAppender">
    <PatternLayout pattern="%m" alwaysWriteExceptions="false"/>
    <DispatcherClass>com.example.MyDispatcher</DispatcherClass>
    <SNMPVersions>2c</SNMPVersions>
</SNMPTrapAppender>
```

The interface used to be in the misspelled package `org.dush.log4j2.intefaces`. That name still works but is deprecated.

## Building
```bash
mvn verify
```
The tests include an end-to-end test that sends real v1 and v2c traps to an in-process SNMP4J receiver on the loopback interface.
