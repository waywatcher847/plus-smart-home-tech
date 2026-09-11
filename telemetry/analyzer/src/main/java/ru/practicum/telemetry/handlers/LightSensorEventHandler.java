package ru.practicum.telemetry.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.enums.SensorClasses;
import ru.practicum.telemetry.repositories.Metrics;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;

import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class LightSensorEventHandler implements SnapshotHandler {
    @Override
    public Class<?> getSensorDataClass() {
        return SensorClasses.LIGHT_SENSOR.getSensorClass();
    }

    @Override
    public Integer handle(ConditionType conditionType, SensorStateAvro sensorsState) {
        LightSensorAvro data = (LightSensorAvro) sensorsState.getData();

        Function<LightSensorAvro, Integer> metricFunction = Metrics.LIGHT_SENSORS_METRICS.get(conditionType);

        if (metricFunction == null) {
            log.warn("ConditionType {} is not applicable to LightSensor. Data: {}", conditionType, data);
            return null;
        }

        return metricFunction.apply(data);
    }
}
