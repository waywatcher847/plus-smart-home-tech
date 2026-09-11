package ru.practicum.telemetry.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.enums.SensorClasses;
import ru.practicum.telemetry.repositories.Metrics;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;

import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class MotionSensorEventHandler implements SnapshotHandler {
    @Override
    public Class<?> getSensorDataClass() {
        return SensorClasses.MOTION_SENSOR.getSensorClass();
    }

    @Override
    public Integer handle(ConditionType conditionType, SensorStateAvro sensorsState) {
        if (conditionType != ConditionType.MOTION) {
            log.warn("ConditionType {} is not applicable to MotionSensor", conditionType);
            return null;
        }

        MotionSensorAvro data = (MotionSensorAvro) sensorsState.getData();
        Function<MotionSensorAvro, Boolean> metricFunction = Metrics.MOTION_SENSORS_METRICS.get(conditionType);

        if (metricFunction == null) {
            return null;
        }

        return metricFunction.apply(data) ? 1 : 0;
    }
}
