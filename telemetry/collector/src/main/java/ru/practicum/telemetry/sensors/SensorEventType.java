package ru.practicum.telemetry.sensors;

public enum SensorEventType {
    CLIMATE_SENSOR_EVENT("CLIMATE_SENSOR_EVENT"),
    LIGHT_SENSOR_EVENT("LIGHT_SENSOR_EVENT"),
    MOTION_SENSOR_EVENT("MOTION_SENSOR_EVENT"),
    SWITCH_SENSOR_EVENT("SWITCH_SENSOR_EVENT"),
    TEMPERATURE_SENSOR_EVENT("TEMPERATURE_SENSOR_EVENT");

    private final String label;

    SensorEventType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
    public String getLabel() {
        return label;
    }

}
