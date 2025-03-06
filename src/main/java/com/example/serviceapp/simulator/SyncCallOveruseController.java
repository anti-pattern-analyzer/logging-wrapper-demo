package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Simulates Synchronous Call Overuse - excessive blocking requests.
 */
@RestController
public class SyncCallOveruseController {
    private final LogService logService;

    public SyncCallOveruseController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Simulate a long synchronous blocking call.
     * @curl curl -X GET "http://localhost:8080/sync-overuse/service"
     */
    @GetMapping("/sync-overuse/service")
    public String syncOverusedService(@RequestParam String input,
                                      @RequestHeader(value = "trace_id", required = false) String traceId,
                                      @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("sync-overuse-service", "database", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        try {
            TimeUnit.SECONDS.sleep(5); // Simulating excessive synchronous call delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String response = "Synchronous Call Overuse detected!";
        logService.log("sync-overuse-service", "database", methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }
}
