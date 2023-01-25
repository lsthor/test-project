package org.example.adapter;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class HttpAdapterTest {
    @Mock
    HttpClient httpClient;

    @Test
    void testCallingGetMethod() throws IOException, InterruptedException {
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(createDummyResponse(200, "ok"));
        var httpAdapter = new HttpAdapter("instance-1", "http://localhost", httpClient);
        var response = httpAdapter.get("/");
        assertEquals(200, response.status());
        assertEquals("ok", response.body());
        assertEquals("instance-1", response.headers().get("x-server"));
    }

    @Test
    void testCallingPostMethod() throws IOException, InterruptedException {
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(createDummyResponse(200, "this is a test"));
        var httpAdapter = new HttpAdapter("instance-1", "http://localhost", httpClient);
        var response = httpAdapter.post("/", "this is a test");
        assertEquals(200, response.status());
        assertEquals("this is a test", response.body());
        assertEquals("instance-1", response.headers().get("x-server"));
    }

    @Test
    void testCallingPutMethod() throws IOException, InterruptedException {
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(createDummyResponse(200, "this is a test"));
        var httpAdapter = new HttpAdapter("instance-1", "http://localhost", httpClient);
        var response = httpAdapter.put("/", "this is a test");
        assertEquals(200, response.status());
        assertEquals("this is a test", response.body());
        assertEquals("instance-1", response.headers().get("x-server"));
    }

    @Test
    void testCallingHeadMethod() throws IOException, InterruptedException {
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(createDummyResponse(200, null));
        var httpAdapter = new HttpAdapter("instance-1", "http://localhost", httpClient);
        var response = httpAdapter.head("/");
        assertEquals(200, response.status());
        assertEquals(null, response.body());
        assertEquals("instance-1", response.headers().get("x-server"));
    }

    @Test
    void testCallingDeleteMethod() throws IOException, InterruptedException {
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass())))
                .thenReturn(createDummyResponse(200, null));
        var httpAdapter = new HttpAdapter("instance-1", "http://localhost", httpClient);
        var response = httpAdapter.delete("/");
        assertEquals(200, response.status());
        assertEquals(null, response.body());
        assertEquals("instance-1", response.headers().get("x-server"));
    }

    HttpResponse<String> createDummyResponse(int statusCode, String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return null;
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return null;
            }
        };
    }
}
