package ru.practicum.telemetry.hubs.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.hubs.types.HubEventType;

@Getter
@Setter
@ToString
public class ScenarioRemovedEvent extends HubEvent {
    private String name;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_REMOVED;
    }
}
