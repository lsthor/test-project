package org.example.adapter;

import lombok.extern.slf4j.Slf4j;
import org.example.respond.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
public class HttpAdapter implements Adapter {
    private final String name;
    private final String url;
    private final HttpClient httpClient;

    public HttpAdapter(String name, String url) {
        this.name = name;
        this.url = url;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1000)).build();
    }

    public HttpAdapter(String name, String url, HttpClient httpClient) {
        this.name = name;
        this.url = url;
        this.httpClient = httpClient;
    }

    @Override
    public Response post(String path, String body) {
        Response response;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + path))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes()))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            response = new Response(httpResponse.statusCode(), httpResponse.body(), Map.of("x-server", name));
        } catch (IOException | InterruptedException e) {
            log.error("Error calling POST method on {}", path, e);
            response = new Response(500, "Error calling url " + url, Map.of("x-server", name));
        }

        return response;
    }

    @Override
    public Response get(String path) {
        Response response;
        log.info("GET {}", url + path);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + path))
                    .GET().build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            response = new Response(httpResponse.statusCode(), httpResponse.body(), Map.of("x-server", name));
        } catch (IOException | InterruptedException e) {
            log.error("Error calling GET method on {}", url + path, e);
            throw new RuntimeException(e);
        }

        return response;
    }

    @Override
    public Response delete(String path) {
        Response response;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + path))
                    .DELETE()
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            response = new Response(httpResponse.statusCode(), httpResponse.body(), Map.of("x-server", name));
        } catch (IOException | InterruptedException e) {
            log.error("Error calling DELETE method on {}", path, e);
            throw new RuntimeException(e);
        }

        return response;
    }

    @Override
    public Response put(String path, String body) {
        Response response;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + path))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(body.getBytes()))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            response = new Response(httpResponse.statusCode(), httpResponse.body(), Map.of("x-server", name));
        } catch (IOException | InterruptedException e) {
            log.error("Error calling PUT method on {}", path, e);
            throw new RuntimeException(e);
        }

        return response;
    }

    @Override
    public Response head(String path) {
        Response response;

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + path))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            response = new Response(httpResponse.statusCode(), httpResponse.body(), Map.of("x-server", name));
        } catch (IOException | InterruptedException e) {
            log.error("Error calling HEAD method on {}", path, e);
            throw new RuntimeException(e);
        }

        return response;
    }

    @Override
    public boolean healthcheck() {
        Response response = get("/");
        return response.status() >= 200 && response.status() < 300;
    }

    @Override
    public String toString() {
        return "HttpAdapter{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
