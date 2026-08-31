package ru.practicum.telemetry;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.telemetry.hubs.events.HubEvent;
import ru.practicum.telemetry.mapper.HubMapper;
import ru.practicum.telemetry.mapper.SensorMapper;
import ru.practicum.telemetry.sensors.SensorEvent;
import ru.practicum.telemetry.telemetry.TelemetryConstants;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsServiceImpl implements EventsService {
    private final Producer<String, SpecificRecordBase> producer;

    @Override
    public void processSensorEvent(SensorEvent sensorEvent) {
        SpecificRecordBase sensor = SensorMapper.toAvro(sensorEvent);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(TelemetryConstants.TELEMETRY_SENSORS_TOPIC, sensorEvent.getId(), sensor);
        producer.send(record);
    }

    @Override
    public void processHubEvent(HubEvent hubEvent) {
        SpecificRecordBase hub = HubMapper.toAvro(hubEvent);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(TelemetryConstants.TELEMETRY_HUBS_TOPIC, hubEvent.getHubId(), hub);
        producer.send(record);
    }

    @PreDestroy
    public void destroy() {
        producer.close();
    }
}
