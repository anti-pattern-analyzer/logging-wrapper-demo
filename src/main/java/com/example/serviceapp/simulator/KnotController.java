package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

/**
 * Simulates "The Knot" - A Tightly Coupled Set of Services.
 */
@RestController
public class KnotController {
    private final LogService logService;
    private final WebClient webClient;

    public KnotController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    @GetMapping("/knot/start")
    public String startKnot(@RequestParam String input,
                            @RequestHeader(value = "trace_id", required = false) String traceId,
                            @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("knot-service-A", "knot-service-B", "initiateKnot", "GET", input, 102, null, traceId, spanId, parentSpanId);
        logService.log("knot-service-A", "knot-service-C", "initiateKnot", "GET", input, 102, null, traceId, spanId, parentSpanId);

        String response = callService("knot-service-A", input, traceId, spanId);

        // **Ensure dense logging**
        logService.log("knot-service-A", "knot-service-B", "completeKnot", "GET", input, 200, response, traceId, spanId, parentSpanId);
        logService.log("knot-service-A", "knot-service-C", "completeKnot", "GET", input, 200, response, traceId, spanId, parentSpanId);

        return response;
    }

    @GetMapping("/knot/{current}")
    public String processKnot(@PathVariable String current,
                              @RequestParam String input,
                              @RequestHeader("trace_id") String traceId,
                              @RequestHeader("span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String next1 = nextService(current, 1);
        String next2 = nextService(current, 2);

        // **Ensure dense logging**
        logService.log(current, next1, "knotProcessing", "GET", input, 102, null, traceId, spanId, parentSpanId);
        logService.log(current, next2, "knotProcessing", "GET", input, 102, null, traceId, spanId, parentSpanId);

        String response = "Tightly coupled services interacting.";

        // **Log both interactions again to reinforce dense relationships**
        logService.log(current, next1, "knotProcessingComplete", "GET", input, 200, response, traceId, spanId, parentSpanId);
        logService.log(current, next2, "knotProcessingComplete", "GET", input, 200, response, traceId, spanId, parentSpanId);

        return response;
    }

    private String callService(String source, String input, String traceId, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();

        logService.log(source, "knot-service-B", "invokeKnotService", "GET", input, 102, null, traceId, spanId, parentSpanId);
        logService.log(source, "knot-service-C", "invokeKnotService", "GET", input, 102, null, traceId, spanId, parentSpanId);

        String response = "Services tangled together.";

        // **Log both interactions again to ensure dense relationships**
        logService.log(source, "knot-service-B", "invokeKnotServiceComplete", "GET", input, 200, response, traceId, spanId, parentSpanId);
        logService.log(source, "knot-service-C", "invokeKnotServiceComplete", "GET", input, 200, response, traceId, spanId, parentSpanId);

        return response;
    }

    private String nextService(String current, int choice) {
        return switch (current) {
            case "knot-service-A" -> (choice == 1) ? "knot-service-B" : "knot-service-C";
            case "knot-service-B" -> (choice == 1) ? "knot-service-C" : "knot-service-A";
            case "knot-service-C" -> (choice == 1) ? "knot-service-A" : "knot-service-B";
            default -> "knot-service-A";
        };
    }
}
