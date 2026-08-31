package ru.practicum.telemetry.hubs.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.hubs.types.HubEventType;

@Getter
@Setter
@ToString
public class DeviceRemovedEvent extends HubEvent {
    private String id;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_REMOVED;
    }
}
