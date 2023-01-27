package org.example.service;

import org.example.request.Request;
import org.example.respond.Response;

public interface RoutingService {
    Response forwardRequest(Request request);

    void stop();
}
