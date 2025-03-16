package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.UUID;

/**
 * Simulates Chatty Services - excessive back-and-forth communication between services.
 */
@RestController
@RequestMapping("/chatty")
public class ChattyServicesController {
    private final LogService logService;
    private final WebClient webClient;
    private static final int CHATTY_THRESHOLD = 15; // Must be greater than 10 to be detected

    public ChattyServicesController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    /**
     * Starts chatty interactions.
     * @curl curl -X GET "http://localhost:8081/chatty/start?input=test"
     */
    @GetMapping("/start")
    public String startChattyInteraction(@RequestParam String input,
                                         @RequestHeader(value = "trace_id", required = false) String traceId,
                                         @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("chatty-service-0", "chatty-service-1", "startChattyInteraction", "GET", input,
                102, null, traceId, spanId, parentSpanId);

        String response = startChattyLoop(input, traceId, spanId, CHATTY_THRESHOLD);

        logService.log("chatty-service-0", null, "startChattyInteraction", "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }

    /**
     * Simulates chatty interactions across multiple unique services.
     */
    private String startChattyLoop(String input, String traceId, String parentSpanId, int threshold) {
        String response = input;
        for (int i = 0; i < threshold; i++) {
            String sourceService = "chatty-service-" + i;
            String destinationService = (i + 1 < threshold) ? "chatty-service-" + (i + 1) : null; // End of flow
            response = callChattyService(sourceService, destinationService, response, traceId, parentSpanId);
        }
        return response;
    }

    /**
     * Simulates chatty communication between services.
     */
    private String callChattyService(String source, String destination, String input, String traceId, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();

        logService.log(source, destination, "invokeChattyService", "GET", input,
                102, null, traceId, spanId, parentSpanId);

        if (destination == null) {
            return source + " reached the end of the chatty interaction.";
        }

        try {
            String response = webClient.get()
                    .uri("/chatty/" + destination + "?input=" + input)
                    .header("trace_id", traceId)
                    .header("span_id", spanId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logService.log(source, destination, "invokeChattyService", "GET", input,
                    200, response, traceId, spanId, parentSpanId);

            return response;
        } catch (Exception e) {
            String errorMessage = "Failed to reach " + destination + ": " + e.getMessage();
            logService.log(source, destination, "error", "GET", input,
                    500, errorMessage, traceId, spanId, parentSpanId);
            return errorMessage;
        }
    }

    /**
     * Generic chatty service handler for any dynamically created service endpoint.
     */
    @GetMapping("/{service}")
    public String genericChattyService(@PathVariable String service,
                                       @RequestParam String input,
                                       @RequestHeader(value = "trace_id") String traceId,
                                       @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = "processChattyService";

        // Determine the next destination
        int serviceNumber = Integer.parseInt(service.replace("chatty-service-", ""));
        String nextService = (serviceNumber + 1 < CHATTY_THRESHOLD) ? "chatty-service-" + (serviceNumber + 1) : null;

        logService.log(service, nextService, methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        String response = service + " processed the request.";

        logService.log(service, nextService, methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }
}
