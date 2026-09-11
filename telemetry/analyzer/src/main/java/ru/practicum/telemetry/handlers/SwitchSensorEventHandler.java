package ru.practicum.telemetry.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.enums.SensorClasses;
import ru.practicum.telemetry.repositories.Metrics;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;

import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwitchSensorEventHandler implements SnapshotHandler {
    @Override
    public Class<?> getSensorDataClass() {
        return SensorClasses.SWITCH_SENSOR.getSensorClass();
    }

    @Override
    public Integer handle(ConditionType conditionType, SensorStateAvro sensorsState) {
        if (conditionType != ConditionType.SWITCH) {
            log.warn("ConditionType {} is not applicable to SwitchSensor", conditionType);
            return null;
        }

        SwitchSensorAvro data = (SwitchSensorAvro) sensorsState.getData();
        Function<SwitchSensorAvro, Boolean> metricFunction = Metrics.SWITCH_SENSORS_METRICS.get(conditionType);

        if (metricFunction == null) {
            return null;
        }

        return metricFunction.apply(data) ? 1 : 0;
    }
}
