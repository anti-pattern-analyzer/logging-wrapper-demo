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
    private static final int MAX_DEPTH = 3;

    public KnotController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    /**
     * Service A -> Calls B & C
     * @curl curl -X GET "http://localhost:8081/knot-a?input=test"
     */
    @GetMapping("/knot-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId,
                           @RequestHeader(value = "depth", required = false, defaultValue = "0") int depth) {

        if (traceId == null) traceId = UUID.randomUUID().toString();
        if (depth >= MAX_DEPTH) return "Max recursion depth reached. Breaking loop.";

        String spanId = UUID.randomUUID().toString();

        logService.log("knot-a", "knot-b", "serviceA", "GET", input, null, traceId, spanId, parentSpanId);
        logService.log("knot-a", "knot-c", "serviceA", "GET", input, null, traceId, spanId, parentSpanId);

        String responseB = callNextService("knot-b", input, traceId, spanId, depth);
        String responseC = callNextService("knot-c", input, traceId, spanId, depth);

        return responseB + " | " + responseC;
    }

    /**
     * Service B -> Calls C
     */
    @GetMapping("/knot-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId,
                           @RequestHeader(value = "depth") int depth) {

        if (depth >= MAX_DEPTH) return "Max recursion depth reached. Breaking loop.";

        String spanId = UUID.randomUUID().toString();
        logService.log("knot-b", "knot-c", "serviceB", "GET", input, null, traceId, spanId, parentSpanId);

        return callNextService("knot-c", input, traceId, spanId, depth);
    }

    /**
     * Service C -> Calls A (forms a tightly coupled network)
     */
    @GetMapping("/knot-c")
    public String serviceC(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId,
                           @RequestHeader(value = "depth") int depth) {

        if (depth >= MAX_DEPTH) return "Max recursion depth reached. Breaking loop.";

        String spanId = UUID.randomUUID().toString();
        logService.log("knot-c", "knot-a", "serviceC", "GET", input, null, traceId, spanId, parentSpanId);

        return callNextService("knot-a", input, traceId, spanId, depth);
    }

    /**
     * Helper method to call the next service while maintaining depth tracking
     */
    private String callNextService(String nextService, String input, String traceId, String parentSpanId, int depth) {
        try {
            return webClient.get()
                    .uri("/" + nextService + "?input=" + input)
                    .header("trace_id", traceId)
                    .header("span_id", UUID.randomUUID().toString())
                    .header("depth", String.valueOf(depth + 1)) // Increment depth
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            return "Error calling " + nextService + ": " + e.getMessage();
        }
    }
}
