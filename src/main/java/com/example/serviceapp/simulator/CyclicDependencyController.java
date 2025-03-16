package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Simulates a Cyclic Dependency (Service A → B → C → A) with manual logging and timeouts.
 */
@RestController
public class CyclicDependencyController {
    private final LogService logService;

    public CyclicDependencyController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Manual logging for cyclic dependency simulation.
     *
     * @curl curl -X GET "http://localhost:8081/cyclic/manual-log?input=test"
     */
    @GetMapping("/cyclic/manual-log")
    public String manualLog(@RequestParam String input) {

        String traceId = UUID.randomUUID().toString();
        String spanA = UUID.randomUUID().toString();
        String spanB = UUID.randomUUID().toString();
        String spanC = UUID.randomUUID().toString();

        // ✅ Service A -> Service B (Start, response = null)
        logService.log("cyclic-service-A", "cyclic-service-B", "cycleStart", "GET", input,
                102, null, traceId, spanA, null);
        sleep();

        // ✅ Service A -> Service B (End, response set)
        logService.log("cyclic-service-A", "cyclic-service-B", "cycleEnd", "GET", input,
                200, "Completed A → B", traceId, spanA, null);
        sleep();

        // ✅ Service B -> Service C (Start, response = null)
        logService.log("cyclic-service-B", "cyclic-service-C", "cycleStart", "GET", input,
                102, null, traceId, spanB, spanA);
        sleep();

        // ✅ Service B -> Service C (End, response set)
        logService.log("cyclic-service-B", "cyclic-service-C", "cycleEnd", "GET", input,
                200, "Completed B → C", traceId, spanB, spanA);
        sleep();

        // ✅ Service C -> Service A (Start, response = null)
        logService.log("cyclic-service-C", "cyclic-service-A", "cycleStart", "GET", input,
                102, null, traceId, spanC, spanB);
        sleep();

        // ✅ Service C -> Service A (End, response set)
        logService.log("cyclic-service-C", "cyclic-service-A", "cycleEnd", "GET", input,
                200, "Completed C → A", traceId, spanC, spanB);
        sleep();

        return "Cyclic dependency simulation logged successfully.";
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
