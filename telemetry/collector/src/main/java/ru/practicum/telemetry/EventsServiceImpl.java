package ru.practicum.telemetry;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.telemetry.hubs.events.HubEvent;
import ru.practicum.telemetry.mapper.HubMapper;
import ru.practicum.telemetry.mapper.SensorMapper;
import ru.practicum.telemetry.sensors.SensorEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsServiceImpl implements EventsService {
    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaEventProducer kafkaEventProducer;

    @Value("${telemetry.topic.sensors}")
    private String sensorsTopic;

    @Value("${telemetry.topic.hubs}")
    private String hubsTopic;


    @Override
    public void processSensorEvent(SensorEvent sensorEvent) {

        SpecificRecordBase sensor = SensorMapper.toAvro(sensorEvent);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(
                        sensorsTopic,
                        null,
                        sensorEvent.getTimestamp().toEpochMilli(),
                        sensorEvent.getHubId(),
                        sensor
                );
        kafkaEventProducer.send(record, createCallback(sensorEvent.getId(), sensorsTopic, "sensor"));
    }

    @Override
    public void processHubEvent(HubEvent hubEvent) {
        SpecificRecordBase hub = HubMapper.toAvro(hubEvent);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(
                        hubsTopic,
                        null,
                        hubEvent.getTimestamp().toEpochMilli(),
                        hubEvent.getHubId(),
                        hub
                );
        kafkaEventProducer.send(record, createCallback(hubEvent.getHubId(), hubsTopic, "hub"));
    }

    private Callback createCallback(String eventId, String topic, String eventType) {
        return (metadata, exception) -> {
            if (exception != null) {
                log.error("{} event fail. id {}. topic {}: {}",
                        eventType, eventId, topic, exception.getMessage(), exception);
            } else {
                log.debug("{} event is sent", eventType);
            }
        };
    }
}
