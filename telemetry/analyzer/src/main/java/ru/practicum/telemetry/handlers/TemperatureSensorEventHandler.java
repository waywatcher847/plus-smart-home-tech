package ru.practicum.telemetry.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.enums.SensorClasses;
import ru.practicum.telemetry.repositories.Metrics;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemperatureSensorEventHandler implements SnapshotHandler {
    @Override
    public Class<?> getSensorDataClass() {
        return SensorClasses.TEMPERATURE_SENSOR.getSensorClass();
    }

    @Override
    public Integer handle(ConditionType conditionType, SensorStateAvro sensorsState) {
        TemperatureSensorAvro data = (TemperatureSensorAvro) sensorsState.getData();
        Function<TemperatureSensorAvro, Integer> metricFunction = Metrics.TEMPERATURE_SENSORS_METRICS.get(conditionType);

        if (metricFunction == null) {
            log.warn("ConditionType {} is not applicable to TemperatureSensor. Data: {}", conditionType, data);
            return null;
        }

        return metricFunction.apply(data);
    }
}