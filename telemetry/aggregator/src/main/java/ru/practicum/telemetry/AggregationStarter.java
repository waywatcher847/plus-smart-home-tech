package ru.practicum.telemetry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationStarter {

    private final Producer<String, SpecificRecordBase> producer;
    private final Consumer<String, SpecificRecordBase> consumer;
    private final SnapshotGenerator snapshotGenerator;

    @Value("${telemetry.topic.sensors}")
    private String sensorsTopic;

    @Value("${telemetry.topic.hubs}")
    private String hubsTopic;

    @Value("${telemetry.topic.snapshots}")
    private String snapshotsTopic;

    public void start() {
        Thread shutdownHook = new Thread(() -> {
            log.info("Shutdown start");
            consumer.wakeup();
        }, "AggregationStarterShutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try {
            consumer.subscribe(List.of(sensorsTopic));

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records =
                        consumer.poll(Duration.ofMillis(5000));

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        processRecord(record);
                    } catch (Exception e) {
                        log.error("</3 Offset processRecord bad {} -> {} / {}",
                                record.topic(), record.partition(), record.offset(), e);
                    }
                }

                try {
                    consumer.commitSync();
                } catch (CommitFailedException e) {
                    log.error("</3 Offset CommitFailed", e);
                }
            }
        } catch (WakeupException e) {
            log.info("<3 AggregationStarter good. off");
        } catch (Exception e) {
            log.error("</3 AggregationStarter bad. idk:", e);
            throw e;
        } finally {
            shutdownResources();
        }
    }

    private void processRecord(ConsumerRecord<String, SpecificRecordBase> record) {
        SpecificRecordBase rawValue = record.value();
        if (!(rawValue instanceof SensorEventAvro event)) {
            return;
        }

        snapshotGenerator.updateState(event).ifPresent(snapshot -> {
            ProducerRecord<String, SpecificRecordBase> snapshotRecord =
                    new ProducerRecord<>(snapshotsTopic, snapshot.getHubId(), snapshot);

            producer.send(snapshotRecord, (metadata, exception) -> {
                if (exception != null) {
                    log.error("</3 producer.send bad hubId={}", snapshot.getHubId(), exception);
                } else {
                    log.debug("</3 producer.sent good. {} -> {} / {}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        });
    }

    private void shutdownResources() {
        try {
            producer.flush();
        } finally {
            try {
                producer.close();
            } finally {
                consumer.close();
            }
        }
    }
}
