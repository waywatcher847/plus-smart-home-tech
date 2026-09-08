package ru.practicum.telemetry;

import ru.practicum.telemetry.hubs.events.HubEvent;
import ru.practicum.telemetry.sensors.SensorEvent;

public interface KafkaSenderService {
    void processSensorEvent(SensorEvent sensorEvent);

    void processHubEvent(HubEvent hubEvent);
}
