package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.adapter.Adapter;
import org.example.request.Request;
import org.example.respond.Response;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class RoundRobinRoutingServiceImpl implements RoutingService{
    public static final Response GATEWAY_TIMEOUT_RESPONSE = new Response(504, "Gateway timeout", Collections.emptyMap());

    private final List<Adapter> adapters;

    private final int timeoutMs;

    private final ExecutorService executorService;

    private int adapterIndex;

    public RoundRobinRoutingServiceImpl(List<Adapter> adapters, int timeoutMs) {
        if(adapters.isEmpty()) {
            throw new IllegalArgumentException("Adapters cannot be empty.");
        }
        this.adapters = adapters;
        this.adapterIndex = 0;
        this.timeoutMs = timeoutMs;
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public Response forwardRequest(Request request) {
        int currentIndex = adapterIndex;
        boolean goNext;
        Response response = null;
        do {
            Adapter nextAdapter = adapters.get(adapterIndex);

            try {
                response = getResponse(request, nextAdapter);
            } catch (Exception e) {
                log.error("Error calling adapter.", e);
            }
            boolean isBackToStartingIndex = (adapterIndex + 1) % adapters.size() == currentIndex % adapters.size();
            if (isBackToStartingIndex) {
                goNext = false;
            } else {
                goNext = response == null;
            }
            adapterIndex = (adapterIndex + 1) % adapters.size();
        } while (goNext);

        if (response == null) {
            return GATEWAY_TIMEOUT_RESPONSE;
        }

        return response;
    }

    private Response getResponse(Request request, Adapter nextAdapter) throws InterruptedException, ExecutionException, TimeoutException {
        Callable<Response> responseCallable = () -> switch (request.method()) {
            case GET -> nextAdapter.get(request.path());
            case POST -> nextAdapter.post(request.path(), request.payload());
            case DELETE -> nextAdapter.delete(request.path());
            case HEAD -> nextAdapter.head(request.path());
            case PUT -> nextAdapter.put(request.path(), request.payload());
        };

        Future<Response> responseFuture = executorService.submit(responseCallable);

        return responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
    }
}
