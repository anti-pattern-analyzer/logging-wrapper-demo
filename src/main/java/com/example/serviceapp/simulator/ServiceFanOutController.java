package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Simulates a Service Fan-out Overload - a single service making excessive calls to multiple downstream services.
 */
@RestController
public class ServiceFanOutController {
    private final LogService logService;
    private final WebClient webClient;

    public ServiceFanOutController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    /**
     * Simulate a service calling too many downstream services.
     * @curl curl -X GET "http://localhost:8080/fan-out/service-main?input=test"
     */
    @GetMapping("/fan-out/service-main")
    public String serviceMain(@RequestParam String input,
                              @RequestHeader(value = "trace_id", required = false) String traceId,
                              @RequestHeader(value = "span_id", required = false) String parentSpanId) {

        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("fan-out-service-main", "fan-out-service-a", methodName, "GET", input, null, traceId, spanId, parentSpanId);
        logService.log("fan-out-service-main", "fan-out-service-b", methodName, "GET", input, null, traceId, spanId, parentSpanId);
        logService.log("fan-out-service-main", "fan-out-service-c", methodName, "GET", input, null, traceId, spanId, parentSpanId);
        logService.log("fan-out-service-main", "fan-out-service-d", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        // Call multiple downstream services
        String responseA = callDownstreamService("fan-out-service-a", input, traceId, spanId);
        String responseB = callDownstreamService("fan-out-service-b", input, traceId, spanId);
        String responseC = callDownstreamService("fan-out-service-c", input, traceId, spanId);
        String responseD = callDownstreamService("fan-out-service-d", input, traceId, spanId);

        String response = "Main service executed fan-out calls: " + responseA + ", " + responseB + ", " + responseC + ", " + responseD;

        logService.log("fan-out-service-main", "final-response", methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }

    /**
     * Simulated downstream services.
     */
    @GetMapping("/fan-out/service-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        return processDownstreamService("fan-out-service-a", input, traceId, parentSpanId);
    }

    @GetMapping("/fan-out/service-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        return processDownstreamService("fan-out-service-b", input, traceId, parentSpanId);
    }

    @GetMapping("/fan-out/service-c")
    public String serviceC(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        return processDownstreamService("fan-out-service-c", input, traceId, parentSpanId);
    }

    @GetMapping("/fan-out/service-d")
    public String serviceD(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        return processDownstreamService("fan-out-service-d", input, traceId, parentSpanId);
    }

    /**
     * Helper method to simulate calling multiple downstream services.
     */
    private String callDownstreamService(String destination, String input, String traceId, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("fan-out-service-main", destination, methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/fan-out/" + destination.replace("fan-out-", "") + "?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log("fan-out-service-main", destination, methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }

    /**
     * Helper method to process responses from downstream services.
     */
    private String processDownstreamService(String service, String input, String traceId, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log(service, null, methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = service + " processed the request.";

        logService.log(service, null, methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }
}
