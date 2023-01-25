package org.example.adapter;

import org.example.respond.Response;

public interface Adapter {
    Response post(String path, String body);

    Response get(String path);

    Response delete(String path);

    Response put(String path, String body);

    Response head(String path);
}
