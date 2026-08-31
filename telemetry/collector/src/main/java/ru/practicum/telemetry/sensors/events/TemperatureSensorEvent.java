package ru.practicum.telemetry.sensors.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.sensors.SensorEvent;
import ru.practicum.telemetry.sensors.SensorEventType;

@Getter
@Setter
@ToString
public class TemperatureSensorEvent extends SensorEvent {
    private int temperatureC;
    private int temperatureF;

    @Override
    public SensorEventType getType() {
        return SensorEventType.TEMPERATURE_SENSOR_EVENT;
    }
}
