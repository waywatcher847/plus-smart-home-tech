package ru.practicum.telemetry.hubs.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.telemetry.hubs.DeviceAction;
import ru.practicum.telemetry.hubs.ConditionScenario;
import ru.practicum.telemetry.hubs.types.HubEventType;

import java.util.List;

@Getter
@Setter
@ToString
public class ScenarioAddedEvent extends HubEvent {
    private String name;
    private List<ConditionScenario> conditions;
    private List<DeviceAction> actions;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_ADDED;
    }
}
