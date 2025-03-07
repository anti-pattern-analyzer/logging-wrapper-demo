package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Simulates Chatty Services - excessive back-and-forth communication between two services.
 */
@RestController
public class ChattyServicesController {
    private final LogService logService;
    private final WebClient webClient;

    public ChattyServicesController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    /**
     * Simulate excessive back-and-forth calls between two services.
     * @curl curl -X GET "http://localhost:8081/chatty/service-a?input=test"
     */
    @GetMapping("/chatty/service-a")
    public String serviceA(@RequestParam String input,
                           @RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("chatty-service-a", "chatty-service-b", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        for (int i = 0; i < 5; i++) { // Excessive back-and-forth calls
            input = callServiceB(input, traceId, spanId);
        }

        logService.log("chatty-service-a", "final-response", methodName, "GET", input, input, traceId, spanId, parentSpanId);
        return input;
    }

    @GetMapping("/chatty/service-b")
    public String serviceB(@RequestParam String input,
                           @RequestHeader(value = "trace_id") String traceId,
                           @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("chatty-service-b", "chatty-service-a", methodName, "GET", input, null, traceId, spanId, parentSpanId);
        return "Response from Service B";
    }

    private String callServiceB(String input, String traceId, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("chatty-service-a", "chatty-service-b", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        return webClient.get()
                .uri("/chatty/service-b?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
