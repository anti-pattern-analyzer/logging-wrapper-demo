package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simulates Eventual Consistency Pitfall - delayed data updates in distributed systems.
 */
@RestController
public class EventualConsistencyController {
    private final LogService logService;
    private final Map<String, String> database = new HashMap<>(); // Simulated database

    public EventualConsistencyController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Simulates writing data with delay before consistency is achieved.
     * @curl curl -X GET "http://localhost:8081/eventual-consistency/write?input=test"
     */
    @GetMapping("/eventual-consistency/write")
    public String writeData(@RequestParam String input,
                            @RequestHeader(value = "trace_id", required = false) String traceId,
                            @RequestHeader(value = "span_id", required = false) String parentSpanId) {

        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("eventual-consistency-write", "database", "writeData", "EVENT", input, null, traceId, spanId, parentSpanId);

        // Introduce artificial delay before updating database (simulating slow replication)
        String finalTraceId = traceId;
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Simulating 5 seconds delay in data propagation
                database.put("latestData", input);
                logService.log("database", "eventual-consistency-write", "updateDB", "EVENT", input, "Updated in DB", finalTraceId, spanId, parentSpanId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return "Write request received. Data will be available soon!";
    }

    /**
     * Simulates a stale read before data consistency is achieved.
     * @curl curl -X GET "http://localhost:8081/eventual-consistency/read"
     */
    @GetMapping("/eventual-consistency/read")
    public String readData(@RequestHeader(value = "trace_id", required = false) String traceId,
                           @RequestHeader(value = "span_id", required = false) String parentSpanId) {

        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("eventual-consistency-read", "database", "readData", "GET", "Fetch latest", null, traceId, spanId, parentSpanId);

        String response = database.getOrDefault("latestData", "Stale Data (Not Updated Yet)");

        logService.log("eventual-consistency-read", "database", "readData", "GET", "Fetch latest", response, traceId, spanId, parentSpanId);
        return response;
    }
}
