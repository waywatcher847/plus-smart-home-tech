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
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class SnapshotProcessor {

    @Value("${telemetry.topic.snapshots}")
    private String snapshotsTopic;

    @Value("${telemetry.consume.timeout-ms:5000}")
    private long consumeTimeoutMs;

    private final Consumer<String, SpecificRecordBase> consumer;
    private final SnapshotProcessorService snapshotProcessorService;

    public SnapshotProcessor(
            @Qualifier("snapshotConsumer") Consumer<String, SpecificRecordBase> consumer,
            SnapshotProcessorService snapshotProcessorService) {
        this.consumer = consumer;
        this.snapshotProcessorService = snapshotProcessorService;
    }

    public void start() {
        Thread shutdownHook = new Thread(() -> {
            log.info("Shutdown signal received, waking up consumer");
            consumer.wakeup();
        }, "snapshot-processor-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            consumer.subscribe(List.of(snapshotsTopic));
            log.info("Subscribed to topic: {}", snapshotsTopic);

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records =
                        consumer.poll(Duration.ofMillis(consumeTimeoutMs));

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        processRecord(record);
                    } catch (Exception e) {
                        log.error("Failed to process snapshot topic={} partition={} offset={} key={}",
                                record.topic(), record.partition(), record.offset(), record.key(), e);
                        break;
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
            log.error("Unexpected error in snapshot processor", e);
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

        if (!(base instanceof SensorsSnapshotAvro snapshot)) {
            log.warn("Ignoring unexpected record type: {}",
                    base == null ? "null" : base.getClass());
            return;
        }

        snapshotProcessorService.processSnapshot(snapshot);
    }
}