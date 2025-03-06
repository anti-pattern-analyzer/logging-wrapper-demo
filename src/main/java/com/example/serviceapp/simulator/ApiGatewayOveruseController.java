package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
     * @curl curl -X GET "http://localhost:8080/api-gateway/overload"
     */
    @GetMapping("/api-gateway/overload")
    public String apiGatewayOverload(@RequestParam String input,
                                     @RequestHeader(value = "trace_id", required = false) String traceId,
                                     @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("api-gateway", "backend-service", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        // Simulating high execution time in API Gateway
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String response = "API Gateway overloaded!";
        logService.log("api-gateway", "backend-service", methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }
}
