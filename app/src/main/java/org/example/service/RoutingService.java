package org.example.service;

import org.example.request.Request;
import org.example.respond.Response;

import java.util.Collection;

public interface RoutingService {
//    void init(Collection<Adapter> adapters);
    Response forwardRequest(Request request);
}
