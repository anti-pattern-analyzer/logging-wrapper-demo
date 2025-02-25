package com.example.serviceapp;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@RestController
public class SyncDemoController {
    private final LogService logService;
    private final WebClient webClient;

    public SyncDemoController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    @GetMapping("/service-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {

        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("service-a", "service-b", "GET", input, "Service A processed", traceId, spanId, parentSpanId);

        // Call Service B with trace_id and span_id
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/service-b").queryParam("input", input).build())
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @GetMapping("/service-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {

        String spanId = UUID.randomUUID().toString();

        logService.log("service-b", "service-c", "GET", input, "Service B processed", traceId, spanId, parentSpanId);

        // Call Service C with trace_id and span_id
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/service-c").queryParam("input", input).build())
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @GetMapping("/service-c")
    public String serviceC(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {

        String spanId = UUID.randomUUID().toString();

        logService.log("service-c", "database", "GET", input, "Service C processed", traceId, spanId, parentSpanId);

        return "Service C processed successfully!";
    }
}
