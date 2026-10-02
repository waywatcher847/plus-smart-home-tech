package ru.practicum.telemetry.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.telemetry.SensorEventDeserializer;
import ru.practicum.telemetry.TelemetryAvroSerializer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;
@Configuration
public class ClientConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.key-serializer}")
    private String keySerializer;

    @Value("${spring.kafka.key-deserializer}")
    private String keyDeserializer;

    @Value("${spring.kafka.producer.config}")
    private String producerIdConfig;

    @Value("${spring.kafka.consumer.config}")
    private String consumerIdConfig;

    @Value("${spring.kafka.group.config}")
    private String groupIdConfig;


    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, TelemetryAvroSerializer.class.getName());
        config.put(ProducerConfig.CLIENT_ID_CONFIG, producerIdConfig);

        return new KafkaProducer<>(config);
    }

    @Bean
    public Consumer<String, SpecificRecordBase> kafkaConsumer() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorEventDeserializer.class.getName());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerIdConfig);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConfig);

        return new KafkaConsumer<>(config);
    }
}
