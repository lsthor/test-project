package org.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.Config;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class ConfigParserTest {
    @Test
    void testParsingExpectedArguments() {
        String[] arguments = new String[]{"instance[http://localhost:8001,http://localhost:8002]", "hostname-1", "8001", "500"};
        Config config = ConfigParser.parse(arguments);
        assertEquals(500, config.getTimeoutMs());
        assertEquals(8001, config.getPort());
        assertEquals("hostname-1", config.getHostname());
        assertTrue(config.getInstances().containsKey("instance"));
        assertEquals(List.of("http://localhost:8001", "http://localhost:8002"), config.getInstances().get("instance"));
    }

    @Test
    void testParsingMinimumArguments() {
        String[] arguments = new String[]{"instance[http://localhost:8001,http://localhost:8002]"};
        Config config = ConfigParser.parse(arguments);
        assertEquals(Config.DEFAULT_TIMEOUT_MILLIS, config.getTimeoutMs());
        assertEquals(Config.DEFAULT_PORT, config.getPort());
        assertEquals(Config.DEFAULT_HOSTNAME, config.getHostname());
        assertTrue(config.getInstances().containsKey("instance"));
        assertEquals(List.of("http://localhost:8001", "http://localhost:8002"), config.getInstances().get("instance"));
    }

    @Test
    void testParsingOneArgumentOnly() {
        String[] arguments = new String[]{"instance[http://localhost:8001,http://localhost:8002]"};
        Config config = ConfigParser.parse(arguments);
        assertEquals(Config.DEFAULT_TIMEOUT_MILLIS, config.getTimeoutMs());
        assertTrue(config.getInstances().containsKey("instance"));
        assertEquals(List.of("http://localhost:8001", "http://localhost:8002"), config.getInstances().get("instance"));
    }

    @Test
    void testParsingEmptyArgument() {
        String[] arguments = new String[]{};
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(arguments));
    }

    @Test
    void testParsingEmptyInstanceValue() {
        String[] arguments = new String[]{"instance[]"};
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(arguments));
    }

    @Test
    void testParsingOneInstanceValue() {
        String[] arguments = new String[]{"instance[http://localhost:8001]"};
        Config config = ConfigParser.parse(arguments);
        assertEquals(Config.DEFAULT_TIMEOUT_MILLIS, config.getTimeoutMs());
        assertTrue(config.getInstances().containsKey("instance"));
        assertEquals(List.of("http://localhost:8001"), config.getInstances().get("instance"));
    }

    @Test
    void testParsingInvalidInstance() {
        String[] arguments = new String[]{"instanc"};
        assertThrows(IllegalArgumentException.class, () -> ConfigParser.parse(arguments));
    }

    @Test
    void testParsingInvalidTimeout() {
        String[] arguments = new String[]{"instance[http://localhost:8001]", "1swd"};
        Config config = ConfigParser.parse(arguments);
        assertEquals(Config.DEFAULT_TIMEOUT_MILLIS, config.getTimeoutMs());
        assertTrue(config.getInstances().containsKey("instance"));
        assertEquals(List.of("http://localhost:8001"), config.getInstances().get("instance"));
    }
}
