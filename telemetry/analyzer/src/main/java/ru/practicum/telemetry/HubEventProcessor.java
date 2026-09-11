package ru.practicum.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.handlers.HubEventHandler;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    @Value("${telemetry.topic.hubs}")
    private String hubsTopic;

    @Value("${telemetry.consume.timeout-ms:5000}")
    private long consumeTimeoutMs;

    private final Consumer<String, SpecificRecordBase> consumer;
    private final Map<String, HubEventHandler> hubEventHandlers;

    public HubEventProcessor(
            @Qualifier("hubConsumer") Consumer<String, SpecificRecordBase> consumer,
            Set<HubEventHandler> handlers) {
        this.consumer = consumer;
        this.hubEventHandlers = handlers.stream()
                .collect(Collectors.toMap(
                        h -> h.getPayloadClass().getName(),
                        Function.identity(),
                        (h1, h2) -> {
                            throw new IllegalStateException(
                                    "Duplicate handler for " + h1.getPayloadClass());
                        }
                ));
        log.info("Registered {} hub event handlers: {}",
                hubEventHandlers.size(), hubEventHandlers.keySet());
    }

    @Override
    public void run() {
        Thread shutdownHook = new Thread(() -> {
            log.info("Shutdown signal received, waking up consumer");
            consumer.wakeup();
        }, "hub-event-processor-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            consumer.subscribe(List.of(hubsTopic));
            log.info("Subscribed to topic: {}", hubsTopic);

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records =
                        consumer.poll(Duration.ofMillis(consumeTimeoutMs));

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        processRecord(record);
                    } catch (Exception e) {
                        log.error("Failed to process record topic={} partition={} offset={}",
                                record.topic(), record.partition(), record.offset(), e);
                    }
                }

                try {
                    consumer.commitSync();
                } catch (CommitFailedException e) {
                    log.error("Offset commit failed", e);
                }
            }
        } catch (WakeupException e) {
            log.info("Consumer wakeup received, shutting down gracefully");
        } catch (Exception e) {
            log.error("Unexpected error in hub event processor", e);
            throw e;
        } finally {
            try {
                consumer.close();
            } catch (Exception e) {
                log.error("Error closing consumer", e);
            }
        }
    }

    private void processRecord(ConsumerRecord<String, SpecificRecordBase> record) {
        SpecificRecordBase base = record.value();

        if (!(base instanceof HubEventAvro hubEvent)) {
            log.warn("Ignoring unexpected record type: {}",
                    base == null ? "null" : base.getClass());
            return;
        }

        Object payload = hubEvent.getPayload();
        if (payload == null) {
            log.warn("Missing payload in HubEventAvro: {}", hubEvent);
            return;
        }

        String payloadClassName = payload.getClass().getName();
        HubEventHandler handler = hubEventHandlers.get(payloadClassName);

        if (handler == null) {
            log.warn("No handler registered for payload type: {}", payloadClassName);
            return;
        }

        try {
            handler.handle(hubEvent);
        } catch (Exception e) {
            throw e;
        }
    }
}