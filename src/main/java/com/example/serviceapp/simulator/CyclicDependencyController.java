package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.UUID;

/**
 * Simulates a Cyclic Dependency where Service A → B → C → A.
 */
@RestController
public class CyclicDependencyController {
    private final LogService logService;
    private final WebClient webClient;

    public CyclicDependencyController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    /**
     * Service A -> Calls Service B
     * @curl curl -X GET "http://localhost:8080/cyclic-a?input=test"
     */
    @GetMapping("/cyclic-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("cyclic-a", "cyclic-b", "serviceA", "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/cyclic-b?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }

    /**
     * Service B -> Calls Service C
     */
    @GetMapping("/cyclic-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();

        logService.log("cyclic-b", "cyclic-c", "serviceB", "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/cyclic-c?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }

    /**
     * Service C -> Calls Service A (creates cycle)
     */
    @GetMapping("/cyclic-c")
    public String serviceC(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();

        logService.log("cyclic-c", "cyclic-a", "serviceC", "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/cyclic-a?input=" + input) // Creates the cycle
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }
}
