package ru.practicum.telemetry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.telemetry.entities.*;
import ru.practicum.telemetry.enums.ActionType;
import ru.practicum.telemetry.enums.ConditionOperation;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.enums.EnumMapper;
import ru.practicum.telemetry.repositories.ActionRepository;
import ru.practicum.telemetry.repositories.ConditionRepository;
import ru.practicum.telemetry.repositories.ScenarioRepository;
import ru.practicum.telemetry.repositories.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DataProcessorServiceImpl implements DataProcessorService {
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Override
    public void addScenario(String hubId, ScenarioAddedEventAvro payload) {
        log.trace("Starting scenario addition for hubId={}", hubId);

        validateScenarioPayload(payload);

        Scenario scenario = getOrCreateScenario(hubId, payload.getName());

        Map<String, Sensor> conditionSensorsMap = getSensorsForConditions(hubId, payload.getConditions());
        setScenarioCondition(scenario, payload.getConditions(), conditionSensorsMap);

        Map<String, Sensor> actionSensorsMap = getSensorsForActions(hubId, payload.getActions());
        setScenarioAction(scenario, payload.getActions(), actionSensorsMap);

        scenarioRepository.save(scenario);
        log.debug("Saved scenario id={}, name={}", scenario.getId(), scenario.getName());
    }

    @Override
    public void removeScenario(String hubId, ScenarioRemovedEventAvro payload) {
        log.trace("Starting scenario removal for hubId={}, name={}", hubId, payload.getName());
        scenarioRepository.deleteByHubIdAndName(hubId, payload.getName());
        log.debug("Removed scenario name={} for hubId={}", payload.getName(), hubId);
    }

    @Override
    public void addSensor(String hubId, DeviceAddedEventAvro payload) {
        log.trace("Starting sensor addition for hubId={}, sensorId={}", hubId, payload.getId());

        if (sensorRepository.existsByIdAndHubId(payload.getId(), hubId)) {
            log.warn("Sensor already exists: id={}, hubId={}", payload.getId(), hubId);
            return;
        }

        Sensor sensor = Sensor.builder()
                .id(payload.getId())
                .hubId(hubId)
                .build();
        Sensor savedSensor = sensorRepository.save(sensor);

        log.debug("Saved sensor id={}, hubId={}", savedSensor.getId(), savedSensor.getHubId());
    }

    @Override
    public void removeSensor(DeviceRemovedEventAvro payload, String hubId) {
        log.trace("Starting sensor removal for hubId={}, sensorId={}", hubId, payload.getId());
        sensorRepository.deleteByIdAndHubId(payload.getId(), hubId);
        log.debug("Removed sensor id={} for hubId={}", payload.getId(), hubId);
    }

    private Scenario getOrCreateScenario(String hubId, String name) {
        return scenarioRepository.findByHubIdAndName(hubId, name)
                .map(existingScenario -> {
                    log.debug("change existing scenario id={}", existingScenario.getId());
                    existingScenario.getConditions().clear();
                    existingScenario.getActions().clear();
                    return existingScenario;
                })
                .orElseGet(() -> {
                    Scenario newScenario = Scenario.builder()
                            .hubId(hubId)
                            .name(name)
                            .build();
                    Scenario saved = scenarioRepository.save(newScenario);
                    log.debug("new scenario id={}", saved.getId());
                    return saved;
                });
    }

    private Map<String, Sensor> getSensorsForConditions(String hubId, List<ScenarioConditionAvro> conditions) {
        List<String> conditionSensorIds = conditions.stream()
                .map(ScenarioConditionAvro::getSensorId)
                .toList();

        return sensorRepository.findSensor(conditionSensorIds, hubId).stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));
    }

    private void setScenarioCondition(Scenario scenario,
                                      List<ScenarioConditionAvro> scenarioConditionsAvro,
                                      Map<String, Sensor> sensorsMap) {
        Set<ScenarioCondition> scenarioConditions = scenarioConditionsAvro.stream()
                .map(conditionAvro -> createScenarioCondition(conditionAvro, scenario, sensorsMap))
                .collect(Collectors.toCollection(HashSet::new));

        scenario.setConditions(scenarioConditions);
    }

    private ScenarioCondition createScenarioCondition(ScenarioConditionAvro conditionAvro,
                                                      Scenario scenario,
                                                      Map<String, Sensor> sensorsMap) {
        System.out.println(sensorsMap);
        Sensor sensor = sensorsMap.get(conditionAvro.getSensorId());
        Condition condition = formCondition(conditionAvro);

        if (sensor == null) {
            log.warn("not found sensor id: {}", conditionAvro.getSensorId());
            throw new IllegalArgumentException("not found sensor id: " + conditionAvro.getSensorId());
        }

        ScenarioConditionId id = ScenarioConditionId.builder()
                .scenarioId(scenario.getId())
                .sensorId(sensor.getId())
                .conditionId(condition.getId())
                .build();

        return ScenarioCondition.builder()
                .id(id)
                .scenario(scenario)
                .sensor(sensor)
                .condition(condition)
                .build();
    }

    private Condition formCondition(ScenarioConditionAvro conditionAvro) {

        ConditionType conditionType = EnumMapper.toAppEnum(ConditionType.values(), conditionAvro.getType().name())
                .orElseThrow(() -> new IllegalArgumentException("unknown condition type " + conditionAvro.getType().name()));

        ConditionOperation conditionOperation = EnumMapper.toAppEnum(ConditionOperation.values(), conditionAvro.getOperation().name())
                .orElseThrow(() -> new IllegalArgumentException("unknown condition operation " + conditionAvro.getOperation().name()));;

        log.debug("Mapping Avro condition type: {} -> App enum: {}",
                conditionAvro.getType().name(), conditionType.name());

        Condition condition = Condition.builder()
                .type(conditionType)
                .operation(conditionOperation)
                .value(setConditionValue(conditionAvro.getValue()))
                .build();

        return conditionRepository.save(condition);
    }

    private Integer setConditionValue(Object value) {
        return switch (value) {
            case null -> null;
            case Number number -> number.intValue();
            case Boolean b -> b ? 1 : 0;
            default ->
                    throw new IllegalArgumentException("not Integer, Boolean, Null " + value.getClass());
        };
    }

    private Map<String, Sensor> getSensorsForActions(String hubId, List<DeviceActionAvro> actions) {
        List<String> actionSensorIds = actions.stream()
                .map(DeviceActionAvro::getSensorId)
                .toList();

        return sensorRepository.findSensor(actionSensorIds, hubId).stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));
    }

    private void setScenarioAction(Scenario scenario,
                                   List<DeviceActionAvro> deviceActionAvro,
                                   Map<String, Sensor> sensorsMap) {
        Set<ScenarioAction> scenarioActions = deviceActionAvro.stream()
                .map(actionAvro -> createScenarioAction(actionAvro, scenario, sensorsMap))
                .collect(Collectors.toCollection(HashSet::new));

        scenario.setActions(scenarioActions);
    }

    private ScenarioAction createScenarioAction(DeviceActionAvro actionAvro,
                                                Scenario scenario,
                                                Map<String, Sensor> sensorsMap) {
        Sensor sensor = sensorsMap.get(actionAvro.getSensorId());
        Action action = formAction(actionAvro);

        ScenarioActionId id = ScenarioActionId.builder()
                .scenarioId(scenario.getId())
                .sensorId(sensor.getId())
                .actionId(action.getId())
                .build();

        return ScenarioAction.builder()
                .id(id)
                .scenario(scenario)
                .sensor(sensor)
                .action(action)
                .build();
    }

    private Action formAction(DeviceActionAvro deviceActionAvro) {
        ActionType actionType = EnumMapper.toAppEnum(ActionType.values(), deviceActionAvro.getType().name())
                .orElseThrow(() -> new IllegalArgumentException("unknown device " + deviceActionAvro.getType().name()));

        Action action = Action.builder()
                .type(actionType)
                .value(deviceActionAvro.getValue())
                .build();

        return actionRepository.save(action);
    }

    private void validateScenarioPayload(ScenarioAddedEventAvro payload) {
        if (payload.getName() == null || payload.getName().isBlank()) {
            throw new IllegalArgumentException("Scenario name null or empty");
        }
        if (payload.getConditions() == null) {
            throw new IllegalArgumentException("Scenario conditions null");
        }
        if (payload.getActions() == null) {
            throw new IllegalArgumentException("Scenario actions null");
        }
    }
}
