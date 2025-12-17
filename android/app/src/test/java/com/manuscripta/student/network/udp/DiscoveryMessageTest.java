package com.manuscripta.student.network.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DiscoveryMessage}.
 */
public class DiscoveryMessageTest {
    private DiscoveryMessage discoveryMessage;

    @Before
    public void setUp() {
        this.discoveryMessage = new DiscoveryMessage(
                "192.168.1.100",
                8080,
                9090
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertNotNull(discoveryMessage);
        assertEquals("192.168.1.100", discoveryMessage.getIpAddress());
        assertEquals(8080, discoveryMessage.getHttpPort());
        assertEquals(9090, discoveryMessage.getTcpPort());
    }

    @Test
    public void testConstructorWithMinimumPortValues() {
        // Port 0 is valid
        DiscoveryMessage msg = new DiscoveryMessage("10.0.0.1", 0, 0);
        assertEquals(0, msg.getHttpPort());
        assertEquals(0, msg.getTcpPort());
    }

    @Test
    public void testConstructorWithMaximumPortValues() {
        // Port 65535 is valid (maximum value)
        DiscoveryMessage msg = new DiscoveryMessage("172.16.0.1", 65535, 65535);
        assertEquals(65535, msg.getHttpPort());
        assertEquals(65535, msg.getTcpPort());
    }

    @Test
    public void testConstructorWithTypicalPortValues() {
        // Test with typical HTTP and TCP ports
        DiscoveryMessage msg = new DiscoveryMessage("127.0.0.1", 443, 8443);
        assertEquals("127.0.0.1", msg.getIpAddress());
        assertEquals(443, msg.getHttpPort());
        assertEquals(8443, msg.getTcpPort());
    }

    @Test
    public void testConstructorWithLocalhostIP() {
        DiscoveryMessage msg = new DiscoveryMessage("127.0.0.1", 8080, 9090);
        assertEquals("127.0.0.1", msg.getIpAddress());
    }

    @Test
    public void testConstructor_nullIpAddress_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage(null, 8080, 9090)
        );
        assertEquals("IP Address cannot be Null", exception.getMessage());
    }

    @Test
    public void testConstructor_emptyIpAddress_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("", 8080, 9090)
        );
        assertEquals("IP Address cannot be Empty", exception.getMessage());
    }

    @Test
    public void testConstructor_invalidIpAddressOctetTooLarge_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.256", 8080, 9090)
        );
        assertEquals("IP Address must be a valid IPv4 address (xxx.xxx.xxx.xxx)", exception.getMessage());
    }

    @Test
    public void testConstructor_invalidIpAddressFormat_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1", 8080, 9090)
        );
        assertEquals("IP Address must be a valid IPv4 address (xxx.xxx.xxx.xxx)", exception.getMessage());
    }

    @Test
    public void testConstructor_invalidIpAddressNonNumeric_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.abc", 8080, 9090)
        );
        assertEquals("IP Address must be a valid IPv4 address (xxx.xxx.xxx.xxx)", exception.getMessage());
    }

    @Test
    public void testConstructor_invalidIpAddressTooManyOctets_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.1.1", 8080, 9090)
        );
        assertEquals("IP Address must be a valid IPv4 address (xxx.xxx.xxx.xxx)", exception.getMessage());
    }

    @Test
    public void testConstructor_invalidIpAddressNegativeOctet_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.-1.1", 8080, 9090)
        );
        assertEquals("IP Address must be a valid IPv4 address (xxx.xxx.xxx.xxx)", exception.getMessage());
    }

    @Test
    public void testConstructor_httpPortNegative_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.1", -1, 9090)
        );
        assertEquals("HTTP Port must be between 0 and 65535 inclusive", exception.getMessage());
    }

    @Test
    public void testConstructor_httpPortTooLarge_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.1", 65536, 9090)
        );
        assertEquals("HTTP Port must be between 0 and 65535 inclusive", exception.getMessage());
    }

    @Test
    public void testConstructor_tcpPortNegative_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.1", 8080, -1)
        );
        assertEquals("TCP Port must be between 0 and 65535 inclusive", exception.getMessage());
    }

    @Test
    public void testConstructor_tcpPortTooLarge_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.1", 8080, 65536)
        );
        assertEquals("TCP Port must be between 0 and 65535 inclusive", exception.getMessage());
    }

    @Test
    public void testConstructor_bothPortsNegative_throwsException() {
        // HTTP port validation happens first
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.1", -1, -1)
        );
        assertEquals("HTTP Port must be between 0 and 65535 inclusive", exception.getMessage());
    }

    @Test
    public void testConstructor_bothPortsTooLarge_throwsException() {
        // HTTP port validation happens first
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new DiscoveryMessage("192.168.1.1", 100000, 100000)
        );
        assertEquals("HTTP Port must be between 0 and 65535 inclusive", exception.getMessage());
    }
}
