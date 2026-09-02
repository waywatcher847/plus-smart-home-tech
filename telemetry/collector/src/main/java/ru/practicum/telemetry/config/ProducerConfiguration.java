package ru.practicum.telemetry.config;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.telemetry.TelemetryAvroSerializer;

import java.util.Properties;

@Configuration
public class ProducerConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;

    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        Properties config = new Properties();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);

        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, TelemetryAvroSerializer.class.getName());

        return new KafkaProducer<>(config);
    }
}