package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

/**
 * Simulates a Long Service Chain - excessive intermediary services before a request is processed.
 */
@RestController
public class LongServiceChainController {
    private final LogService logService;
    private final WebClient webClient;

    public LongServiceChainController(LogService logService, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    /**
     * Entry point of the long service chain.
     * @curl curl -X GET "http://localhost:8081/long-chain/start?input=test"
     */
    @GetMapping("/long-chain/start")
    public String startLongChain(@RequestParam String input,
                                 @RequestHeader(value = "trace_id", required = false) String traceId,
                                 @RequestHeader(value = "span_id", required = false) String parentSpanId) {
        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        String methodName = "initiateLongChain";

        logService.log("long-chain-start-service", "long-chain-middle1-service", methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/long-chain/middle1?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log("long-chain-start-service", "long-chain-middle1-service", methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }

    @GetMapping("/long-chain/middle1")
    public String middleService1(@RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        return callNextService("long-chain-middle1-service", "long-chain-middle2-service", input, traceId, parentSpanId);
    }

    @GetMapping("/long-chain/middle2")
    public String middleService2(@RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        return callNextService("long-chain-middle2-service", "long-chain-middle3-service", input, traceId, parentSpanId);
    }

    @GetMapping("/long-chain/middle3")
    public String middleService3(@RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        return callNextService("long-chain-middle3-service", "long-chain-middle4-service", input, traceId, parentSpanId);
    }

    @GetMapping("/long-chain/middle4")
    public String middleService4(@RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        return callNextService("long-chain-middle4-service", "long-chain-middle5-service", input, traceId, parentSpanId);
    }

    @GetMapping("/long-chain/middle5")
    public String middleService5(@RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        return callNextService("long-chain-middle5-service", "long-chain-end-service", input, traceId, parentSpanId);
    }

    /**
     * Final service in the chain that actually processes the request.
     */
    @GetMapping("/long-chain/end")
    public String endService(@RequestParam String input,
                             @RequestHeader(value = "trace_id") String traceId,
                             @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = "finalizeLongChain";

        logService.log("long-chain-end-service", null, methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        String response = "Final service processed the request.";

        logService.log("long-chain-end-service", null, methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }

    /**
     * Helper method to log and call the next service in the chain.
     */
    private String callNextService(String currentService, String nextService, String input, String traceId, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = "process" + currentService.replace("long-chain-", "").replace("-service", "");

        logService.log(currentService, nextService, methodName, "GET", input,
                102, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/long-chain/" + nextService.replace("long-chain-", "").replace("-service", "") + "?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log(currentService, nextService, methodName, "GET", input,
                200, response, traceId, spanId, parentSpanId);

        return response;
    }
}
