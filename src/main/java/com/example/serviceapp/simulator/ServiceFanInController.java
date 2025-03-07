package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.bind.annotation.RestController;

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
     * @curl curl -X GET "http://localhost:8081/fan-in/service-b?input=test"
     * @curl curl -X GET "http://localhost:8081/fan-in/service-c?input=test"
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
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("fan-in-overloaded", null, methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = "Overloaded service processed the request.";

        logService.log("fan-in-overloaded", null, methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }

    /**
     * Helper method to simulate multiple services calling the overloaded service.
     */
    private String callOverloadedService(String sourceService, String input, String traceId, String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log(sourceService, "fan-in-overloaded", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/fan-in/overloaded?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log(sourceService, "fan-in-overloaded", methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }
}
