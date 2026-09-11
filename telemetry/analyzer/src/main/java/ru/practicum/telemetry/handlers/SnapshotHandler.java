package ru.practicum.telemetry.handlers;

import ru.practicum.telemetry.enums.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;

public interface SnapshotHandler {
    Class<?> getSensorDataClass();

    Integer handle(ConditionType conditionType, SensorStateAvro sensorsState);
}
