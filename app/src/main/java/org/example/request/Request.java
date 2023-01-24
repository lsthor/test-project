package org.example.request;

public interface Request {
    Method method();

    String path();

    String payload();
}
