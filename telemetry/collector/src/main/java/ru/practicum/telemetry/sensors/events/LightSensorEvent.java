package ru.practicum.telemetry.sensors.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.sensors.SensorEvent;
import ru.practicum.telemetry.sensors.SensorEventType;

@Getter
@Setter
@ToString
public class LightSensorEvent extends SensorEvent {
    private int linkQuality;
    private int luminosity;

    @Override
    public SensorEventType getType() {
        return SensorEventType.LIGHT_SENSOR_EVENT;
    }
}
