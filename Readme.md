# log4j2-snmp-appender

Log4j2 appender to send SNMP trap messages. Use org.snmp4j [https://www.snmp4j.org/](https://www.snmp4j.org/) as underlying implementation. Underlying implementation can be extend by a custom implementation. see usage for details.   

## other dependencies
```maven
     <dependency>
            <groupId>org.snmp4j</groupId>
            <artifactId>snmp4j</artifactId>
            <version>2.5.0</version>
    </dependency>
```
## Usage 
sample log4j2.xml 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="ALL">
    <Appenders>
        <Console name="ConsoleAppender" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss,SSS} [%t] %-5p %c{36} - %m%n"/>
        </Console>

        <!--Setting up SNMPTrapAppender -->
        <SNMPTrapAppender name="SNMPTrapAppender" >

            <!--This pattern will be send with varbinding.
            For example "SNMPv2-SMI::enterprises.24.12.10.22.64 = 13:59:30,017 [main] INFO TEST - info!"-->
            <PatternLayout pattern="%d{HH:mm:ss,SSS} [%t] %-5p %c{36} - %m%n"/>

            <!--if true prints debugging information on console(system.print())-->
            <verbose>true</verbose>

            <!--SNMP version. Applicable values are 1,2c-->
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

            <!--Value of the port that will be used to send traps out from the local host.The standard is 161-->
            <LocalTrapSendPort>161</LocalTrapSendPort>

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

            <!-- Applicable for SNMP v1, An INTEGER in (value between -128 to 127)-->
            <SpecificTrapType>123</SpecificTrapType>

            <!--community string for the SNMP session. E.g. "public".-->
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

#### Using custom implementation
By default this appender use [SNMP4j](https://www.snmp4j.org)  libraries to send SNMP Trap requests. Can use custom implementation by extending the interface `SNMPDispatchable` provided by this library.

```java
package org.dush.log4j2.intefaces;

/**
 * Can have custom implemention by implementing this interface.
 * For example if the SNMP message should be send using different SNMP library/vendor such as `wengsoft net-snmp` java library
 * implement this interface and add the fully qualified name of the concrete calass into log4j configuration. And note any third party
 * libraries should be available int he calsspath.
 *
 * @author dushmantha (<a href="mailto:dushmanthab99@gmail.com">dushmanthab99@gmail.com</a>)
 * date 2018-nov-12
 */
public interface SNMPDispatchable
{
	//@formatter:off
	/**
	 *
	 * sends SNMP request.
	 * @param message         Log message coming from logger,
	 *                        Eg. if log line in java code is like <l>logger.info(this is a log message)</il>
	 *                        then the @param message would be like <i>2018-11-23 [main.test.TestClass][info] this is a log message</i>.
	 *
	 * @param exceptionStackTrace
	 * 						  When logger has an {@link Exception e}, the stackTrace of 'e' is represented by exceptionStackTrace.
	 * 						  example use of this could be, to put exception stack trace into SNMP varbings.
	 *                        pass null if dont want process exceptions.
	 *
	 */
	//@formatter:on
	void dispatch( String message, String[] exceptionStackTrace ) throws Exception;

	/**
	 * implement this method to to close resources.
	 * For example if implementing class use any connections,can add `connection.close()` inside this method.
	 *
	 * @throws Exception can exceptions when releasing resources. Eg. IOException
	 */
	void releaseResources() throws Exception;

}

```
