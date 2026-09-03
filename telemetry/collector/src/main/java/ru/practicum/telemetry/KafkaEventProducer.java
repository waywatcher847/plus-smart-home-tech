package ru.practicum.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaEventProducer implements AutoCloseable {
    private final Producer<String, SpecificRecordBase> producer;

    public KafkaEventProducer(Producer<String, SpecificRecordBase> producer) {
        this.producer = producer;
    }

    public void send(ProducerRecord<String, SpecificRecordBase> record,
                     org.apache.kafka.clients.producer.Callback callback) {
        producer.send(record, callback);
    }

    @Override
    public void close() {
        log.info("Closing Kafka producer");
        try {
            producer.close();
        } catch (Exception e) {
            log.error("Error closing Kafka producer", e);
        }
    }
}