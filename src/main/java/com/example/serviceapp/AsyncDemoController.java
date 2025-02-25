package com.example.serviceapp;

import com.example.loggingwrapper.LogService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@RestController
public class AsyncDemoController {
    private final LogService logService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WebClient webClient;
    private static final String TRACE_TOPIC = "async-trace-logs";

    public AsyncDemoController(LogService logService, KafkaTemplate<String, String> kafkaTemplate, WebClient.Builder webClientBuilder) {
        this.logService = logService;
        this.kafkaTemplate = kafkaTemplate;
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
    }

    // 1. Async Call - Service A sends a Kafka event**
    @GetMapping("/async-service-a")
    public String asyncServiceA(@RequestParam String input,
                                @RequestHeader(value = "trace_id", required = false) String traceId,
                                @RequestHeader(value = "span_id", required = false) String parentSpanId) {

        if (traceId == null) traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        logService.log("async-service-a", "kafka-async-event", "EVENT", input, "Service A initiated async event", traceId, spanId, parentSpanId);

        // Send async event to Kafka instead of calling HTTP directly
        String message = String.format("trace_id=%s, span_id=%s, parent_span_id=%s, input=%s", traceId, spanId, parentSpanId, input);
        kafkaTemplate.send(TRACE_TOPIC, traceId, message);

        return "Async event sent from Service A!";
    }

    // 2. Kafka Consumer - Listens for the event, then calls sync Service B**
    @KafkaListener(topics = "async-trace-logs", groupId = "service-b-group")
    public void listenForAsyncEvent(ConsumerRecord<String, String> record) {
        String message = record.value();
        String traceId = record.key();
        String parentSpanId = extractSpanId(message);
        String spanId = UUID.randomUUID().toString();

        logService.log("kafka-consumer", "sync-service-b", "EVENT", message, "Kafka event received", traceId, spanId, parentSpanId);

        // 3. Calls a synchronous HTTP API (sync-service-b) after consuming event**
        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/sync-service-b").queryParam("input", message).build())
                .header("trace_id", traceId)
                .header("span_id", spanId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        logService.log("kafka-consumer", "sync-service-b", "HTTP", message, response, traceId, spanId, parentSpanId);
    }

    // 4. Synchronous Service B - Normal HTTP endpoint**
    @GetMapping("/sync-service-b")
    public String syncServiceB(@RequestParam String input,
                               @RequestHeader(value = "trace_id") String traceId,
                               @RequestHeader(value = "span_id") String parentSpanId) {

        String spanId = UUID.randomUUID().toString();
        logService.log("sync-service-b", "database", "HTTP", input, "Service B processed synchronously", traceId, spanId, parentSpanId);

        return "Service B processed synchronously!";
    }

    private String extractSpanId(String message) {
        return message.split(", span_id=")[1].split(",")[0];
    }
}
