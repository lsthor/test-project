package org.example.request;

public record RequestImpl(Method method, String path, String payload) implements Request {
    @Override
    public Method method() {
        return method;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String payload() {
        return payload;
    }
}
