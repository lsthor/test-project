package org.example.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.example.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@UtilityClass
@Slf4j
public class ConfigParser {
    public Config parse(String[] arguments) {
        if (arguments.length == 0) {
            throw new IllegalArgumentException("Need at least one argument for instance configuration.");
        }
        String instanceArg = arguments[0];
        String hostname = arguments.length > 1 ? arguments[1] : Config.DEFAULT_HOSTNAME;
        int port = getPortFromArgOrDefault(arguments, Config.DEFAULT_PORT);
        long timeoutMs = getTimeoutMsFromArgOrDefault(arguments, Config.DEFAULT_TIMEOUT_MILLIS);
        Map<String, List<String>> instance = parseStringIntoMap(instanceArg);
        return new Config(instance, timeoutMs, hostname, port);
    }

    private static int getPortFromArgOrDefault(String[] arguments, int defaultPort) {
        if (arguments.length > 2) {
            if (arguments[2].matches("^[0-9]*$")) {
                return Integer.parseInt(arguments[2]);
            }
        }

        log.info("Port is invalid or not set, will fallback to default at {}", defaultPort);
        return defaultPort;
    }

    private static Map<String, List<String>> parseStringIntoMap(String instanceArgs) {
        if (instanceArgs.matches("^[\\w\\d-]*\\[(http\\:\\/\\/[\\w\\d\\:\\,]*)*\\]")) {
            String name = instanceArgs.substring(0, instanceArgs.indexOf("["));
            String instanceValue = instanceArgs.substring(instanceArgs.indexOf("[") + 1, instanceArgs.indexOf("]"));
            if (instanceValue.trim().length() == 0) {
                throw new IllegalArgumentException("Need at least one instance value.");
            }
            List<String> instances = Arrays.stream(instanceValue.split(",")).map(String::trim).toList();
            return Map.of(name, instances);
        } else {
            throw new IllegalArgumentException("Instance argument format is invalid.");
        }
    }

    private static long getTimeoutMsFromArgOrDefault(String[] arguments, long defaultTimeoutMillis) {
        if (arguments.length > 3) {
            if (arguments[3].matches("^[0-9]*$")) {
                return Long.parseLong(arguments[3]);
            }
        }

        log.info("Timeout is invalid or not set, will fallback to default at {}", defaultTimeoutMillis);
        return defaultTimeoutMillis;
    }
}
