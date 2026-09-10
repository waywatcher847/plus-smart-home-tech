package ru.practicum.telemetry.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_conditions")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioCondition {
    @EmbeddedId
    private ScenarioConditionId id;

    @MapsId("scenarioId")
    @JoinColumn(name = "scenario_id")
    @ManyToOne
    private Scenario scenario;

    @MapsId("sensorId")
    @JoinColumn(name = "sensor_id")
    @ManyToOne
    private Sensor sensor;

    @MapsId("conditionId")
    @JoinColumn(name = "condition_id")
    @ManyToOne
    private Condition condition;
}
