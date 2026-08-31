package ru.practicum.telemetry.sensors.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.sensors.SensorEvent;
import ru.practicum.telemetry.sensors.SensorEventType;

@Getter
@Setter
@ToString
public class ClimateSensorEvent extends SensorEvent {
    private int temperatureC;
    private int humidity;
    private int co2Level;

    @Override
    public SensorEventType getType() {
        return SensorEventType.CLIMATE_SENSOR_EVENT;
    }
}
