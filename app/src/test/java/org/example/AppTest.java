package org.example;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.extern.slf4j.Slf4j;
import org.example.utils.ConfigParser;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import static org.awaitility.Awaitility.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@Slf4j
public class AppTest {
    @Rule
    public Network network = Network.newNetwork();
    @Container
    public ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
            .withNetwork(network);
    @Container
    public GenericContainer<?> simpleService1 = new GenericContainer<>(DockerImageName.parse("simple-service"))
            .withExposedPorts(8000)
            .withNetwork(network)
            .withNetworkAliases("simple-service-1");
    @Container
    public GenericContainer<?> simpleService2 = new GenericContainer<>(DockerImageName.parse("simple-service"))
            .withExposedPorts(8000);
    private int port1;
    private String address1;
    private int port2;
    private String address2;
    private Proxy proxy1;


    @BeforeEach
    public void setUp() throws IOException {
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());

        proxy1 = toxiproxyClient.createProxy("simple-service-1", "0.0.0.0:8666", "simple-service-1:8000");
        address1 = simpleService1.getHost();
        port1 = simpleService1.getMappedPort(8000);
        address2 = simpleService2.getHost();
        port2 = simpleService2.getMappedPort(8000);
    }

    @Test
    public void testAppCreateAndNormalRouting() throws IOException {
        String[] instances = new String[]{"http://" + address1 + ":" + port1, "http://" + address2 + ":" + port2};
        Config config = ConfigParser.parse(new String[]{"instance[" + instances[0] + "," + instances[1] + "]", "localhost", "8000", "500"});
        App app = new App(config);
        IntStream.range(0, 2).forEach(index -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8000"))
                    .GET()
                    .build();
            HttpResponse<String> response = null;
            try {
                response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertEquals("ok", response.body());
                assertTrue(response.headers().map().containsKey("x-server"));
//                assertTrue(response.headers().map().get("x-server").get(0).contains(instances[index]));
            } catch (IOException | InterruptedException e) {
                log.error("{}", e.getMessage());
            }
        });
        app.stop();
    }

    @Test
    public void testPostPayload() throws IOException {
        String body = "{\"game\":\"Mobile Legends\", \"gamerID\":\"GYUTDTE\", \"points\":20}";
        String[] instances = new String[]{"http://" + address1 + ":" + port1, "http://" + address2 + ":" + port2};
        Config config = ConfigParser.parse(new String[]{"instance[" + instances[0] + "," + instances[1] + "]", "localhost", "8000", "500"});
        App app = new App(config);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8000"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes()))
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals(body, response.body());
            assertTrue(response.headers().map().containsKey("x-server"));
        } catch (IOException | InterruptedException e) {
            log.error("{}", e.getMessage());
        }
        app.stop();
    }

    @Test
    public void testAppCreateAndOneInstanceFailing() throws IOException {
        proxy1.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 800);
        String[] instances = new String[]{"http://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(8666), "http://" + address2 + ":" + port2};
        Config config = ConfigParser.parse(new String[]{"instance[" + instances[0] + "," + instances[1] + "]", "localhost", "8000", "1000"});
        App app = new App(config);
        IntStream.range(0, 4).forEach(index -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8000"))
                    .GET()
                    .build();
            HttpResponse<String> response = null;
            try {
                response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                log.error("{}", e.getMessage());
            }
            assertNotNull(response);
            assertEquals(200, response.statusCode());
            assertEquals("ok", response.body());
            assertTrue(response.headers().map().containsKey("x-server"));
        });
        app.stop();
    }

    @Test
    public void testRecoveringInstance() throws IOException {
        proxy1.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 800);
        String[] instances = new String[]{"http://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(8666), "http://" + address2 + ":" + port2};
        Config config = ConfigParser.parse(new String[]{"instance[" + instances[0] + "," + instances[1] + "]", "localhost", "8000", "1000"});
        App app = new App(config);
        IntStream.range(0, 3).forEach(index -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8000"))
                    .GET()
                    .build();
            HttpResponse<String> response = null;
            try {
                response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                log.error("{}", e.getMessage());
            }
            assertNotNull(response);
            assertEquals(200, response.statusCode());
            assertEquals("ok", response.body());
            assertTrue(response.headers().map().containsKey("x-server"));
            //should only return from second instance
            assertTrue(response.headers().map().get("x-server").get(0).contains(instances[1]));
        });
        proxy1.toxics().get("timeout").remove();
        //wait until first instance is recovered
        await().pollInterval(1000, TimeUnit.MILLISECONDS).atMost(Duration.of(10, ChronoUnit.SECONDS)).until(() -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8000"))
                    .GET()
                    .build();
            HttpResponse<String> response = null;
            try {
                response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                return response.headers().map().get("x-server").get(0).contains(instances[0]);
            } catch (IOException | InterruptedException e) {
                log.error("{}", e.getMessage());
                return false;
            }
        });
        app.stop();
    }

    @Test
    public void testNoHealthyInstance() throws IOException {
        proxy1.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 800);
        String[] instances = new String[]{"http://" + toxiproxy.getHost() + ":" + toxiproxy.getMappedPort(8666)};
        Config config = ConfigParser.parse(new String[]{"instance[" + instances[0] + "]", "localhost", "8000", "1000"});
        App app = new App(config);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:8000"))
                .GET()
                .build();
        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("huh? {}", e.getMessage());
        }
        log.info("{}", response);
        assertNotNull(response);
        assertEquals(504, response.statusCode());
        app.stop();
    }
}
