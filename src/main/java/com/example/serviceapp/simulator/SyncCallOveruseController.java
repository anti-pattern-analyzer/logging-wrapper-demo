package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.time.Duration;

import java.util.UUID;

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
     *
     * @curl curl -X GET "http://localhost:8081/sync-overuse/service?input=test"
     */

    @GetMapping("/sync-overuse/service")
    public Mono<String> syncOverusedService(@RequestParam String input,
                                            @RequestHeader(value = "trace_id", required = false) String traceId,
                                            @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("sync-overuse-service", "database", "syncOveruse", "GET", input, null, traceId, spanId, parentSpanId);

        String finalTraceId = traceId;
        return Mono.delay(Duration.ofSeconds(5))
                .map(ignored -> {
                    String response = "Synchronous Call Overuse detected!";
                    logService.log("sync-overuse-service", "database", "syncOveruse", "GET", input, response, finalTraceId, spanId, parentSpanId);
                    return response;
                });
    }

}
