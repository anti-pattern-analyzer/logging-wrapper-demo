package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates a Service Fan-out Overload - a single service making excessive calls to multiple downstream services.
 */
@RestController
@RequestMapping("/fan-out")
public class ServiceFanOutController {
    private final LogService logService;
    private final WebClient webClient;

    // Simulated database to handle service responses
    private final Map<String, String> serviceResponses = new ConcurrentHashMap<>();

    public ServiceFanOutController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    /**
     * Simulate a service calling too many downstream services.
     * @curl curl -X GET "http://localhost:8081/fan-out/service-main?input=test"
     */
    @GetMapping("/service-main")
    public String serviceMain(@RequestParam String input,
                              @RequestHeader(value = "trace_id", required = false) String traceId,
                              @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = "executeFanOut";

        logService.log("fan-out-main-service", "fan-out-multiple-services", methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        // Handling multiple downstream service calls
        String[] downstreamServices = {"service-a", "service-b", "service-c", "service-d"};
        StringBuilder finalResponse = new StringBuilder("Fan-out responses: ");

        for (String service : downstreamServices) {
            finalResponse.append(callDownstreamService(service, input, traceId, spanId)).append(", ");
        }

        String response = finalResponse.toString().replaceAll(", $", ""); // Remove trailing comma
        logService.log("fan-out-main-service", "fan-out-multiple-services", methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }

    /**
     * Handles dynamic downstream services: A, B, C, D.
     * @curl curl -X GET "http://localhost:8081/fan-out/service-a?input=test"
     */
    @GetMapping("/{service}")
    public String dynamicService(@PathVariable String service,
                                 @RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = "processDownstreamService";

        logService.log(service, null, methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        // Simulated processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate and store response
        String response = service + " processed the request.";
        serviceResponses.put(service, response);

        logService.log(service, null, methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }

    /**
     * Simulates calling a downstream service.
     */
    private String callDownstreamService(String service, String input, String traceId, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = "invokeDownstreamService";

        logService.log("fan-out-main-service", service, methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        try {
            String response = webClient.get()
                    .uri("/fan-out/" + service + "?input=" + input)
                    .header("trace_id", traceId)
                    .header("span_id", spanId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logService.log("fan-out-main-service", service, methodName, "GET", input,
                    200, response, traceId, spanId, parentSpanId);

            return response;
        } catch (Exception e) {
            String errorMessage = "Failed to reach " + service + ": " + e.getMessage();
            logService.log("fan-out-main-service", service, "error", "GET", input,
                    500, errorMessage, traceId, spanId, parentSpanId);
            return errorMessage;
        }
    }
}
