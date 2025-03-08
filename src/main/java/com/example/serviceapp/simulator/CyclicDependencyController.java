package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Simulates a Cyclic Dependency where Service A → B → C → A.
 */
@RestController
public class CyclicDependencyController {
    private final LogService logService;
    private final WebClient webClient;
    private static final int MAX_DEPTH = 3;

    public CyclicDependencyController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://serviceapp:8081").build();
    }

    /**
     * Simulate cyclic dependencies
     * @curl curl -X GET "http://localhost:8081/cyclic/cyclic-a?input=test"
     */
    @GetMapping("/cyclic/cyclic-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId,
                           @RequestHeader(value = "depth", required = false, defaultValue = "0") int depth) {

        if (traceId == null) traceId = UUID.randomUUID().toString(); // Ensure trace_id is never null
        if (depth >= MAX_DEPTH) return "Max recursion depth reached. Breaking cycle.";

        return callNextService("cyclic-a", "cyclic-b", input, traceId, parentSpanId, depth);
    }

    @GetMapping("/cyclic/cyclic-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId,
                           @RequestHeader(value = "depth") int depth) {

        if (depth >= MAX_DEPTH) return "Max recursion depth reached. Breaking cycle.";
        return callNextService("cyclic-b", "cyclic-c", input, traceId, parentSpanId, depth);
    }

    @GetMapping("/cyclic/cyclic-c")
    public String serviceC(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId,
                           @RequestHeader(value = "depth") int depth) {

        if (depth >= MAX_DEPTH) return "Max recursion depth reached. Breaking cycle.";
        return callNextService("cyclic-c", "cyclic-d", input, traceId, parentSpanId, depth);
    }

    /**
     * Introduce cyclic-d to complete the loop
     */
    @GetMapping("/cyclic/cyclic-d")
    public String serviceD(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId,
                           @RequestHeader(value = "depth") int depth) {

        if (depth >= MAX_DEPTH) return "Max recursion depth reached. Breaking cycle.";
        return callNextService("cyclic-d", "cyclic-a", input, traceId, parentSpanId, depth); // Loop back to A
    }

    /**
     * Helper method to call the next service while maintaining depth tracking
     */
    private String callNextService(String source, String destination, String input, String traceId, String parentSpanId, int depth) {
        if (depth >= MAX_DEPTH) {
            return "Max recursion depth reached. Breaking cycle.";
        }

        String spanId = UUID.randomUUID().toString();
        logService.log(source, destination, "callNextService", "GET", input, null, traceId, spanId, parentSpanId);

        try {
            return webClient.get()
                    .uri("/cyclic/" + destination + "?input=" + input)
                    .header("trace_id", traceId)
                    .header("span_id", spanId)
                    .header("depth", String.valueOf(depth + 1))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            return "Error calling " + destination + ": " + e.getMessage();
        }
    }
}
