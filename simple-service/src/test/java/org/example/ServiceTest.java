package org.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ExtendWith(SystemStubsExtension.class)
public class ServiceTest {
    @SystemStub
    private EnvironmentVariables environmentVariables;

    private static void callService(String uri, String body, int expectedStatus) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(new URI(uri))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(expectedStatus, response.statusCode());
        assertEquals(body, response.body());
    }

    @ParameterizedTest
    @MethodSource("testServicePortFromArgsProvider")
    public void testServicePortFromArgs(String envPort, String expectedPort) throws URISyntaxException, IOException, InterruptedException {
        log.info("calling port {}", expectedPort);
        environmentVariables.set("SERVICE_PORT", envPort);
        var service = new Service();
        String body = "{\"game\":\"Mobile Legends\", \"gamerID\":\"GYUTDTE\", \"points\":20}";
        callService("http://localhost:" + expectedPort, body, 200);
        service.stop();
    }

    @Test
    public void testServiceConnection() throws URISyntaxException, IOException, InterruptedException {
        var service = Service.builder().port(8000).build();
        String body = "{\"game\":\"Mobile Legends\", \"gamerID\":\"GYUTDTE\", \"points\":20}";
        callService("http://localhost:8000", body, 200);
        service.stop();
    }

    private static Stream<Arguments> testServicePortFromArgsProvider() {
        return Stream.of(
                Arguments.of("8010", "8010"),
                Arguments.of("100", "8000"),
                Arguments.of("10091", "10091"),
                Arguments.of(null, "8000"),
                Arguments.of("invalid-num", "8000"),
                Arguments.of("", "8000"),
                Arguments.of(" ", "8000")
        );
    }
}
