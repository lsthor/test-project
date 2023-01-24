package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.adapter.Adapter;
import org.example.request.Request;
import org.example.respond.Response;

import java.util.Collections;
import java.util.List;

@Slf4j
public class RoundRobinRoutingServiceImpl implements RoutingService{
    public static final Response GATEWAY_TIMEOUT_RESPONSE = new Response(504, "Gateway timeout", Collections.emptyMap());

    private final List<Adapter> adapters;

    private int adapterIndex;

    public RoundRobinRoutingServiceImpl(List<Adapter> adapters) {
        if(adapters.isEmpty()) {
            throw new IllegalArgumentException("Adapters cannot be empty.");
        }
        this.adapters = adapters;
        this.adapterIndex = 0;
    }

    @Override
    public Response forwardRequest(Request request) {
        int currentIndex = adapterIndex;
        boolean goNext;
        Response response = null;
        do {
            Adapter nextAdapter = getNextAdapter();
            try {
                response = switch (request.method()) {
                    case GET -> nextAdapter.get(request.path());
                    case POST -> nextAdapter.post(request.path(), request.payload());
                    case DELETE -> nextAdapter.delete(request.path());
                    case PATCH -> nextAdapter.patch(request.path(), request.payload());
                    case HEAD -> nextAdapter.head(request.path());
                    case PUT -> nextAdapter.put(request.path(), request.payload());
                };
            } catch (Exception e) {
                log.error("{}", e.getMessage(), e);
            }

            if ((adapterIndex + 1) % adapters.size() == currentIndex % adapters.size()) {
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

    private Adapter getNextAdapter() {
        return adapters.get(adapterIndex);
    }
}
