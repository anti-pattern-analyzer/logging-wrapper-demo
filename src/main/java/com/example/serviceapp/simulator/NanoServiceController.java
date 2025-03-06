package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

/**
 * Simulates a Nano Service - a microservice that is too small to be useful.
 */
@RestController
public class NanoServiceController {
    private final LogService logService;

    public NanoServiceController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Nano Service - performs a trivial operation
     * @curl curl -X GET "http://localhost:8080/nano-service?input=test"
     */
    @GetMapping("/nano-service")
    public String nanoService(@RequestParam String input,
                              @RequestHeader(value = "trace_id", required = false) String traceId,
                              @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("nano-service", null, methodName, "GET", input, null, traceId, spanId, parentSpanId);

        // Nano services often just return static or trivial values
        String response = "Nano service executed a simple task.";

        logService.log("nano-service", null, methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }
}
