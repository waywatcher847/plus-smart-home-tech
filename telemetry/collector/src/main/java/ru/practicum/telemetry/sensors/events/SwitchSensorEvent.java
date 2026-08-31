package ru.practicum.telemetry.sensors.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.sensors.SensorEvent;
import ru.practicum.telemetry.sensors.SensorEventType;

@Getter
@Setter
@ToString
public class SwitchSensorEvent extends SensorEvent {
    private boolean state;

    @Override
    public SensorEventType getType() {
        return SensorEventType.SWITCH_SENSOR_EVENT;
    }
}
