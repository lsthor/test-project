package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.adapter.Adapter;
import org.example.request.Request;
import org.example.respond.Response;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;

@Slf4j
public class RoundRobinRoutingServiceImpl implements RoutingService {
    public static final Response GATEWAY_TIMEOUT_RESPONSE = new Response(504, "Gateway timeout", Collections.emptyMap());

    private final List<Adapter> adapters;

    private final long timeoutMs;

    private final ExecutorService executorService;

    private final ScheduledExecutorService executor;
    private final AtomicReferenceArray<Boolean> problematicAdapterIndex;
    private int adapterIndex;

    public RoundRobinRoutingServiceImpl(List<Adapter> adapters, long timeoutMs) {
        if (adapters.isEmpty()) {
            throw new IllegalArgumentException("Adapters cannot be empty.");
        }
        this.adapters = adapters;
        this.adapterIndex = 0;
        this.timeoutMs = timeoutMs;
        Boolean[] arr = new Boolean[adapters.size()];
        Arrays.fill(arr, Boolean.FALSE);

        problematicAdapterIndex = new AtomicReferenceArray<>(arr);
        executorService = Executors.newSingleThreadExecutor();
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            OptionalInt optionalInt = IntStream.range(0, arr.length)
                    .filter(index -> problematicAdapterIndex.get(index).equals(Boolean.TRUE))
                    .findFirst();
            if (optionalInt.isPresent()) {
                boolean healthy = adapters.get(optionalInt.getAsInt()).healthcheck();
                if (healthy) {
                    problematicAdapterIndex.set(optionalInt.getAsInt(), Boolean.FALSE);
                }
            }
        }, 0, Duration.ofSeconds(1).toSeconds(), TimeUnit.SECONDS);

    }

    @Override
    public void stop() {
        executorService.shutdown();
        executor.shutdown();
    }

    @Override
    public Response forwardRequest(Request request) {
        int currentIndex = adapterIndex;
        boolean goNext;
        Response response = null;
        do {
            if (Boolean.FALSE.equals(problematicAdapterIndex.get(adapterIndex))) {
                Adapter nextAdapter = adapters.get(adapterIndex);
                try {
                    response = getResponse(request, nextAdapter);
                } catch (Exception e) {
                    log.error("Error calling adapter.");
                    problematicAdapterIndex.set(adapterIndex, Boolean.TRUE);
                }

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
            log.info("wwwwwssssss {}", GATEWAY_TIMEOUT_RESPONSE);
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
