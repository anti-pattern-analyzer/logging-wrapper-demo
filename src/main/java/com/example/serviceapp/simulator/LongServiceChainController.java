package com.example.serviceapp.simulator;

import com.example.loggingwrapper.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.bind.annotation.RestController;

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
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("long-chain-start", "long-chain-middle1", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/long-chain/middle1?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log("long-chain-start", "long-chain-middle1", methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }

    /**
     * Middle service in the long chain.
     */
    @GetMapping("/long-chain/middle1")
    public String middleService1(@RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("long-chain-middle1", "long-chain-middle2", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/long-chain/middle2?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log("long-chain-middle1", "long-chain-middle2", methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }

    /**
     * Another intermediary service before reaching the final one.
     */
    @GetMapping("/long-chain/middle2")
    public String middleService2(@RequestParam String input,
                                 @RequestHeader(value = "trace_id") String traceId,
                                 @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("long-chain-middle2", "long-chain-end", methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = webClient.get()
                .uri("/long-chain/end?input=" + input)
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log("long-chain-middle2", "long-chain-end", methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }

    /**
     * Final service in the chain that actually processes the request.
     */
    @GetMapping("/long-chain/end")
    public String endService(@RequestParam String input,
                             @RequestHeader(value = "trace_id") String traceId,
                             @RequestHeader(value = "span_id") String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

        logService.log("long-chain-end", null, methodName, "GET", input, null, traceId, spanId, parentSpanId);

        String response = "Final service processed the request.";

        logService.log("long-chain-end", null, methodName, "GET", input, response, traceId, spanId, parentSpanId);
        return response;
    }
}
