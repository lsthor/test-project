package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.example.adapter.Adapter;
import org.example.request.Method;
import org.example.request.Request;
import org.example.request.RequestImpl;
import org.example.respond.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.example.service.RoundRobinRoutingServiceImpl.GATEWAY_TIMEOUT_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class RoutingServiceTest {
    private static Stream<Arguments> provideRequestForward() {
        return Stream.of(
                Arguments.of(new RequestImpl(Method.GET, "/", ""),
                        (Consumer<Adapter>) (Adapter adapter) -> when(adapter.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1"))),
                        (Consumer<Adapter>) (Adapter adapter) -> verify(adapter, times(1)).get("/")),
                Arguments.of(new RequestImpl(Method.POST, "/", "body"),
                        (Consumer<Adapter>) (Adapter adapter) -> when(adapter.post(anyString(), anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1"))),
                        (Consumer<Adapter>) (Adapter adapter) -> verify(adapter, times(1)).post("/", "body")),
                Arguments.of(new RequestImpl(Method.PUT, "/", "body"),
                        (Consumer<Adapter>) (Adapter adapter) -> when(adapter.put(anyString(), anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1"))),
                        (Consumer<Adapter>) (Adapter adapter) -> verify(adapter, times(1)).put("/", "body")),
                Arguments.of(new RequestImpl(Method.DELETE, "/", ""),
                        (Consumer<Adapter>) (Adapter adapter) -> when(adapter.delete(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1"))),
                        (Consumer<Adapter>) (Adapter adapter) -> verify(adapter, times(1)).delete("/")),
                Arguments.of(new RequestImpl(Method.HEAD, "/", ""),
                        (Consumer<Adapter>) (Adapter adapter) -> when(adapter.head(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1"))),
                        (Consumer<Adapter>) (Adapter adapter) -> verify(adapter, times(1)).head("/"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideRequestForward")
    void testRequestForwardedCorrectly(Request request, Consumer<Adapter> adapterConsumer, Consumer<Adapter> verifyMethodCall) {
        Adapter adapter = mock(Adapter.class);
        adapterConsumer.accept(adapter);
        RoutingService routingService = new RoundRobinRoutingServiceImpl(List.of(adapter), 1000);
        routingService.forwardRequest(request);
        verifyMethodCall.accept(adapter);
    }

    @Test
    void testRequestRoutingForwardedCorrectly() {
        Request req = new RequestImpl(Method.GET, "/", "");
        Adapter adapter1 = mock(Adapter.class);
        when(adapter1.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1")));
        Adapter adapter2 = mock(Adapter.class);
        when(adapter2.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-2")));
        RoutingService routingService = new RoundRobinRoutingServiceImpl(List.of(adapter1, adapter2), 1000);
        routingService.forwardRequest(req);
        routingService.forwardRequest(req);
        routingService.forwardRequest(req);
        verify(adapter1, times(2)).get(anyString());
        verify(adapter2, times(1)).get(anyString());
    }

    @Test
    void testRequestRoutingRespondCorrectly() {
        Request req = new RequestImpl(Method.GET, "/", "");
        Adapter adapter1 = mock(Adapter.class);
        when(adapter1.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1")));
        Adapter adapter2 = mock(Adapter.class);
        when(adapter2.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-2")));
        RoutingService routingService = new RoundRobinRoutingServiceImpl(List.of(adapter1, adapter2), 1000);
        routingService.forwardRequest(req);
        routingService.forwardRequest(req);
        routingService.forwardRequest(req);
        verify(adapter1, times(2)).get(anyString());
        verify(adapter2, times(1)).get(anyString());
    }

    @Test
    void testRequestRoutingRespondCorrectlyWithSomeAdaptersFailing() {
        Adapter adapter1 = mock(Adapter.class);
        when(adapter1.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-1")));
        Adapter adapter2 = mock(Adapter.class);
        when(adapter2.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-2")));
        Adapter adapter3 = mock(Adapter.class);
        when(adapter3.get(anyString())).thenReturn(new Response(200, "ok", Map.of("x-server", "server-3")));
        RoutingService routingService = new RoundRobinRoutingServiceImpl(List.of(adapter1, adapter2, adapter3), 1000);
        routingService.forwardRequest(new RequestImpl(Method.GET, "/", "req 1"));
        routingService.forwardRequest(new RequestImpl(Method.GET, "/", "req 2"));
        routingService.forwardRequest(new RequestImpl(Method.GET, "/", "req 3"));
        //should route to first adapter again, but this round we will make it fail
        doThrow(new RuntimeException("some error")).when(adapter1).get(anyString());
        //this following call should call next instance when first is failing
        var response = routingService.forwardRequest(new RequestImpl(Method.GET, "/", "req 4"));
        assertEquals("server-2", response.headers().get("x-server"));
        verify(adapter1, times(2)).get(anyString());
        verify(adapter2, times(2)).get(anyString());
    }

    @Test
    void testRequestRoutingRespondCorrectlyWithAllAdaptersFailing() {
        Adapter adapter1 = mock(Adapter.class);
        Adapter adapter2 = mock(Adapter.class);
        Adapter adapter3 = mock(Adapter.class);
        RoutingService routingService = new RoundRobinRoutingServiceImpl(List.of(adapter1, adapter2, adapter3), 1000);
        // making all adapter fail
        doThrow(new RuntimeException("some error")).when(adapter1).get(anyString());
        doThrow(new RuntimeException("some error")).when(adapter2).get(anyString());
        doThrow(new RuntimeException("some error")).when(adapter3).get(anyString());
        var response = routingService.forwardRequest(new RequestImpl(Method.GET, "/", "req 1"));
        assertEquals(GATEWAY_TIMEOUT_RESPONSE, response);
    }

    @Test
    void testRequestRoutingRespondWithTimeoutException() {
        Adapter adapter1 = mock(Adapter.class);
        RoutingService routingService = new RoundRobinRoutingServiceImpl(List.of(adapter1), 500);
        // making all adapter fail
        when(adapter1.get(anyString())).thenAnswer((Answer<Response>) invocation -> {
            Thread.sleep(700);
            return new Response(200, "ok", Map.of("x-server", "server-1"));
        });
        var response = routingService.forwardRequest(new RequestImpl(Method.GET, "/", "req 1"));
        assertEquals(GATEWAY_TIMEOUT_RESPONSE, response);
    }

    @Test
    void shouldNotInstantiateWithEmptyAdapterList() {
        assertThrows(IllegalArgumentException.class, () -> new RoundRobinRoutingServiceImpl(Collections.emptyList(), 1000));
    }

    @Test
    void shouldTakeProblematicInstanceOffline() {
        Adapter adapter1 = mock(Adapter.class);
        Adapter adapter2 = mock(Adapter.class);
        when(adapter2.get(anyString())).thenAnswer((Answer<Response>) invocation -> {
            Thread.sleep(200);
            return new Response(200, "ok", Map.of("x-server", "server-2"));
        });
        RoutingService routingService = new RoundRobinRoutingServiceImpl(List.of(adapter1, adapter2), 5000);
        doThrow(new RuntimeException("some error")).when(adapter1).get(anyString());
        IntStream.range(0, 2)
                .forEach(index -> routingService
                        .forwardRequest(new RequestImpl(Method.GET, "/", "req " + index)));
        verify(adapter1, times(1)).get(anyString());
        verify(adapter2, times(2)).get(anyString());
        reset(adapter1);

        when(adapter1.get(anyString())).thenAnswer((Answer<Response>) invocation -> {
            Thread.sleep(200);
            return new Response(200, "ok", Map.of("x-server", "server-1"));
        });
        when(adapter1.healthcheck()).thenAnswer((Answer<Boolean>) invocation -> true);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        var response = routingService
                .forwardRequest(new RequestImpl(Method.GET, "/", "req 3"));
        assertEquals("server-1", response.headers().get("x-server"));
        routingService.stop();
    }
}
