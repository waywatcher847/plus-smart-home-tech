package ru.practicum.telemetry.handlers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.DataProcessorService;
import ru.practicum.telemetry.enums.HubEventClasses;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
@RequiredArgsConstructor
public class DeviceRemovedEventHandler implements HubEventHandler {
    private final DataProcessorService dataProcessorService;

    @Override
    public Class<?> getPayloadClass() {
        return HubEventClasses.REMOVE_DEVICE.getEventClass();
    }


    @Override
    public void handle(Object hubEvent) {
        HubEventAvro event = (HubEventAvro) hubEvent;
        DeviceRemovedEventAvro payload = (DeviceRemovedEventAvro) event.getPayload();
        dataProcessorService.removeSensor(payload, event.getHubId());
    }
}
