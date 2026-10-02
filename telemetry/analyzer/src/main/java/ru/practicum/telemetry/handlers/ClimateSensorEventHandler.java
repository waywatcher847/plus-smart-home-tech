package ru.practicum.telemetry.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.enums.SensorClasses;
import ru.practicum.telemetry.repositories.Metrics;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;


import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClimateSensorEventHandler implements SnapshotHandler {

    @Override
    public Class<?> getSensorDataClass() {
        return SensorClasses.CLIMATE_SENSOR.getSensorClass();
    }

    @Override
    public Integer handle(ConditionType conditionType, SensorStateAvro sensorsState) {
        ClimateSensorAvro data = (ClimateSensorAvro) sensorsState.getData();

        Function<ClimateSensorAvro, Integer> metricFunction = Metrics.CLIMATE_SENSORS_METRICS.get(conditionType);


        if (metricFunction == null) {
            log.warn("ConditionType {} is not applicable to ClimateSensor. Data: {}", conditionType, data);
            return null;
        }

        return metricFunction.apply(data);
    }
}