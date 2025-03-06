package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.UUID;

/**
 * Simulates The Knot - tightly coupled microservices.
 */
@RestController
public class KnotController {
    private final LogService logService;
    private final WebClient webClient;

    public KnotController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    /**
     * Service A -> Calls B & C
     * @curl curl -X GET "http://localhost:8080/knot-a?input=test"
     */
    @GetMapping("/knot-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("knot-a", "knot-b", "serviceA", "GET", input, null, traceId, spanId, parentSpanId);
        logService.log("knot-a", "knot-c", "serviceA", "GET", input, null, traceId, spanId, parentSpanId);

        String responseB = webClient.get().uri("/knot-b?input=" + input).header("trace_id", traceId).header("span_id", spanId).retrieve().bodyToMono(String.class).block();
        String responseC = webClient.get().uri("/knot-c?input=" + input).header("trace_id", traceId).header("span_id", spanId).retrieve().bodyToMono(String.class).block();

        return responseB + " | " + responseC;
    }

    /**
     * Service B -> Calls C
     */
    @GetMapping("/knot-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();

        logService.log("knot-b", "knot-c", "serviceB", "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get().uri("/knot-c?input=" + input).header("trace_id", traceId).header("span_id", spanId).retrieve().bodyToMono(String.class).block();
        return response;
    }

    /**
     * Service C -> Calls A (forms a tightly coupled network)
     */
    @GetMapping("/knot-c")
    public String serviceC(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();

        logService.log("knot-c", "knot-a", "serviceC", "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get().uri("/knot-a?input=" + input).header("trace_id", traceId).header("span_id", spanId).retrieve().bodyToMono(String.class).block();
        return response;
    }
}
