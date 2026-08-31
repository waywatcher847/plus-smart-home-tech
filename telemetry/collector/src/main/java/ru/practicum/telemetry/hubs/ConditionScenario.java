package ru.practicum.telemetry.hubs;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.hubs.types.ConditionType;

@Getter
@Setter
@ToString
public class ConditionScenario {
    private String sensorId;
    private ConditionType type;
    private ConditionOperation operation;
    private Object value;
}
