package ru.practicum.telemetry.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.enums.SensorClasses;
import ru.practicum.telemetry.repositories.Metrics;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

@Component
@RequiredArgsConstructor
public class TemperatureSensorEventHandler implements SnapshotHandler {
    @Override
    public Class<?> getSensorDataClass() {
        return SensorClasses.TEMPERATURE_SENSOR.getSensorClass();
    }

    @Override
    public int handle(ConditionType conditionType, SensorStateAvro sensorsState) {
        TemperatureSensorAvro data = (TemperatureSensorAvro) sensorsState.getData();
        return Metrics.TEMPERATURE_SENSORS_METRICS.get(conditionType).apply(data);
    }
}
