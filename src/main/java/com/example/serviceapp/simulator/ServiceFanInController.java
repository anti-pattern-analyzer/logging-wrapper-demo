package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

/**
 * Simulates a Service Fan-in Overload - multiple services making requests to a single overloaded service.
 */
@RestController
public class ServiceFanInController {
    private final LogService logService;
    private final WebClient webClient;

    public ServiceFanInController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    /**
     * Simulate multiple upstream services calling the overloaded service.
     * @curl curl -X GET "http://localhost:8081/fan-in/service-a?input=test"
     */
    @GetMapping("/fan-in/service-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        return callOverloadedService("fan-in-service-a", input, traceId, parentSpanId);
    }

    @GetMapping("/fan-in/service-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        return callOverloadedService("fan-in-service-b", input, traceId, parentSpanId);
    }

    @GetMapping("/fan-in/service-c")
    public String serviceC(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        return callOverloadedService("fan-in-service-c", input, traceId, parentSpanId);
    }

    /**
     * The overloaded service that receives too many requests.
     */
    @GetMapping("/fan-in/overloaded")
    public String overloadedService(@RequestParam String input,
                                    @RequestHeader(value = "trace_id") String traceId,
                                    @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = "processOverloadedService";

        // Log before processing with `102 Processing`
        logService.log("fan-in-overloaded-service", null, methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        try {
            Thread.sleep(1000); // Simulating slight delay in overloaded service
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String response = "Overloaded service processed the request.";

        // Log after processing with `200 OK`
        logService.log("fan-in-overloaded-service", null, methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }

    /**
     * Helper method to simulate multiple services calling the overloaded service.
     */
    private String callOverloadedService(String sourceService, String input, String traceId, String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = "invokeOverloadedService";

        // Log before making the request with `102 Processing`
        logService.log(sourceService, "fan-in-overloaded-service", methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/fan-in/overloaded?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Log after receiving the response with `200 OK`
        logService.log(sourceService, "fan-in-overloaded-service", methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }
}
