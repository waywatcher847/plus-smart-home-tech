package ru.practicum.telemetry.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.telemetry.TelemetryAvroSerializer;

import java.util.Map;

@Configuration
public class ProducerConfiguration {

    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer(
            KafkaProperties kafkaProperties,
            ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers) {

        Map<String, Object> props = kafkaProperties.buildProducerProperties((SslBundles) customizers);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, TelemetryAvroSerializer.class.getName());

        return new KafkaProducer<>(props);
    }
}