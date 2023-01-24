package org.example.respond;

import java.util.Map;

public record Response(int status, String body, Map<String, String> headers){}