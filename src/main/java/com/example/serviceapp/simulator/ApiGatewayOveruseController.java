package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Simulates Improper API Gateway Usage - overloading the gateway with long-running requests.
 */
@RestController
public class ApiGatewayOveruseController {
    private final LogService logService;

    public ApiGatewayOveruseController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Simulate API Gateway overload scenario.
     *
     * @curl curl -X GET "http://localhost:8081/api-gateway/overload?input=test"
     */
    @GetMapping("/api-gateway/overload")
    public CompletableFuture apiGatewayOverload(@RequestParam String input,
                                                @RequestHeader(value = "trace_id", required = false) String traceId,
                                                @RequestHeader(value = "span_id", required = false) String parentSpanId) {

        // Ensure variables are final/effectively final
        final String finalTraceId = (traceId == null) ? UUID.randomUUID().toString() : traceId;
        final String finalParentSpanId = (parentSpanId == null) ? UUID.randomUUID().toString() : parentSpanId;
        final String finalSpanId = UUID.randomUUID().toString();

        return CompletableFuture.supplyAsync(() -> {
            logService.log("api-gateway", "backend-service", "apiGatewayOverload", "GET", input, null, finalTraceId, finalSpanId, finalParentSpanId);

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String response = "API Gateway overloaded!";
            logService.log("api-gateway", "backend-service", "apiGatewayOverload", "GET", input, response, finalTraceId, finalSpanId, finalParentSpanId);
            return response;
        });
    }
}
