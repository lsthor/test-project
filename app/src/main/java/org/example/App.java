package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.example.adapter.Adapter;
import org.example.adapter.HttpAdapter;
import org.example.request.Method;
import org.example.request.RequestImpl;
import org.example.respond.Response;
import org.example.service.RoundRobinRoutingServiceImpl;
import org.example.service.RoutingService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Slf4j
public class App {
    private final RoutingService routingService;
    private final HttpServer httpServer;
    public App(Config config) throws IOException {
        List<Adapter> adapters = config.getInstances().entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream().map(url -> (Adapter) new HttpAdapter(entry.getKey() + "[" + url + "]", url)))
                .toList();
        routingService = new RoundRobinRoutingServiceImpl(adapters, config.getTimeoutMs());
        httpServer = HttpServer.create(new InetSocketAddress(config.getHostname(), config.getPort()), 0);
        httpServer.createContext("/", new MyHttpHandler());
        httpServer.start();
    }

    public void stop(){
        routingService.stop();
        httpServer.stop(0);
    }

    private class MyHttpHandler implements HttpHandler {
        private void respond(HttpExchange httpExchange, int status, String body, Map<String, List<String>> headers) throws IOException {
            var outputStream = httpExchange.getResponseBody();
            if (headers != null) {
                httpExchange.getResponseHeaders().putAll(headers);
            }
            httpExchange.sendResponseHeaders(status, body.length());
            outputStream.write(body.getBytes());
            outputStream.flush();
            outputStream.close();
        }


        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            if ("GET".equals(httpExchange.getRequestMethod())) {
                Response response = routingService.forwardRequest(new RequestImpl(Method.GET, "/", ""));
                log.info("in app {}", response);
                Map<String, List<String>> headers = Collections.emptyMap();
                if (!response.headers().isEmpty()) {
                    headers = Map.of("x-server", List.of(response.headers().get("x-server")));
                }
                respond(httpExchange, response.status(), response.body(), headers);
            } else if ("POST".equals(httpExchange.getRequestMethod())) {
                // do client post
                Scanner sc = new Scanner(httpExchange.getRequestBody());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) {
                    sb.append(sc.nextLine());
                }
                Response response = routingService.forwardRequest(new RequestImpl(Method.POST, "/", sb.toString()));
                Map<String, List<String>> headers = Collections.emptyMap();
                if (!response.headers().isEmpty()) {
                    headers = Map.of("x-server", List.of(response.headers().get("x-server")));
                }
                respond(httpExchange, response.status(), response.body(), headers);
            }
        }


    }
}
