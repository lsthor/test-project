package org.example;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public class Config {
    public static final long DEFAULT_TIMEOUT_MILLIS = 1000L;
    public static final int DEFAULT_PORT = 8000;
    public static final String DEFAULT_HOSTNAME = "localhost";
    private final Map<String, List<String>> instances;
    private final long timeoutMs;
    private final String hostname;
    private final int port;
}
