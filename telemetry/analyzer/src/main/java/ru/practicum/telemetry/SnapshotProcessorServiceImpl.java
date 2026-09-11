package ru.practicum.telemetry;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.telemetry.entities.*;
import ru.practicum.telemetry.enums.ConditionOperation;
import ru.practicum.telemetry.enums.ConditionType;
import ru.practicum.telemetry.handlers.SnapshotHandler;
import ru.practicum.telemetry.repositories.ScenarioRepository;
import ru.practicum.telemetry.repositories.Metrics;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SnapshotProcessorServiceImpl implements SnapshotProcessorService {
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;
    private final Map<Class<?>, SnapshotHandler> snapshotHandlers;
    private final ScenarioRepository scenarioRepository;

    public SnapshotProcessorServiceImpl(@GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient,
                                        Set<SnapshotHandler> snapshotHandlers,
                                        ScenarioRepository scenarioRepository) {
        this.hubRouterClient = hubRouterClient;
        this.snapshotHandlers = snapshotHandlers.stream()
                .collect(Collectors.toMap(SnapshotHandler::getSensorDataClass, Function.identity()));
        this.scenarioRepository = scenarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void processSnapshot(SensorsSnapshotAvro sensorsSnapshotAvro) {
        log.trace("processSnapshot {}", sensorsSnapshotAvro);
        String hubId = sensorsSnapshotAvro.getHubId();
        Map<String, SensorStateAvro> sensorsState = sensorsSnapshotAvro.getSensorsState();

        if (sensorsState == null || sensorsState.isEmpty()) {
            log.warn("skip empty Snapshot for hubId={}", hubId);
            return;
        }

        List<DeviceActionRequest> actions = buildActionsForSnapshot(hubId, sensorsState);

        sendActionsToHub(hubId, actions);
    }

    protected List<DeviceActionRequest> buildActionsForSnapshot(
            String hubId, Map<String, SensorStateAvro> sensorsState) {

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.debug("Found {} scenarios for hubId={}", scenarios.size(), hubId);

        return scenarios.stream()
                .filter(scenario -> evaluateConditions(scenario, sensorsState))
                .flatMap(scenario -> scenario.getActions().stream()
                        .map(action -> buildAction(scenario, action)))
                .toList();
    }

    private boolean evaluateConditions(Scenario scenario, Map<String, SensorStateAvro> sensorsState) {
        return scenario.getConditions().stream()
                .allMatch(condition -> matchCondition(condition, sensorsState));
    }

    private boolean matchCondition(ScenarioCondition scenarioCondition, Map<String, SensorStateAvro> sensorsState) {
        Sensor sensor = scenarioCondition.getSensor();
        Condition condition = scenarioCondition.getCondition();
        ConditionType conditionType = condition.getType();
        SensorStateAvro state = sensorsState.get(sensor.getId());
        log.debug("sensor: {}, condition: {}, conditionType: {}, state: {}", sensor, condition, conditionType, state);


        if (state == null) {
            log.debug("state = null for sensorId={}", sensor.getId());
            return false;
        }

        Integer sensorsData = getSensorsData(conditionType, state);

        if (sensorsData == null) {
            log.debug("ConditionType {} is not supported by the actual sensor data type. Condition failed.", conditionType);
            return false;
        }

        int conditionValue = condition.getValue() != null ? condition.getValue() : 0;
        ConditionOperation operation = condition.getOperation();
        log.debug("conditionValue: {}, sensorsData: {}, operation: {}", conditionValue, sensorsData, operation);

        boolean comparisonResult = Metrics.COMPARATORS.get(operation).apply(sensorsData, conditionValue);
        log.debug("comparisonResult: {}", comparisonResult);
        return comparisonResult;
    }

    private Integer getSensorsData(ConditionType conditionType, SensorStateAvro state) {
        log.trace("getSensorsData {}, {}", conditionType, state);
        Class<?> sensorClass = state.getData().getClass();

        if (snapshotHandlers.containsKey(sensorClass)) {
            return snapshotHandlers.get(sensorClass).handle(conditionType, state);
        } else {
            log.warn("Unknown sensor class: {}", sensorClass);
            return null;
        }
    }


    private DeviceActionRequest buildAction(Scenario scenario, ScenarioAction scenarioAction) {
        Sensor sensor = scenarioAction.getSensor();
        Action action = scenarioAction.getAction();

        DeviceActionProto deviceActionProto = DeviceActionProto.newBuilder()
                .setSensorId(sensor.getId())
                .setType(action.getType().toProto())
                .setValue(action.getValue() != null ? action.getValue() : 0)
                .build();
        log.debug("deviceActionProto: {}", deviceActionProto);

        Instant instant = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(scenario.getHubId())
                .setScenarioName(scenario.getName())
                .setAction(deviceActionProto)
                .setTimestamp(timestamp)
                .build();
        log.debug("request: {}", request);

        return request;
    }

    private void sendActionsToHub(String hubId, List<DeviceActionRequest> actions) {
        if (actions.isEmpty()) {
            log.debug("No actions to send for hubId={}", hubId);
            return;
        }

        log.debug("Sending {} actions to hubId={}", actions.size(), hubId);

        for (DeviceActionRequest request : actions) {
            try {
                hubRouterClient
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .handleDeviceAction(request);
                log.debug("Sent action for sensorId={} to hubId={}",
                        request.getAction().getSensorId(), hubId);
            } catch (StatusRuntimeException e) {
                log.error("Failed to send action to hubId={}, sensorId={}: {}",
                        hubId, request.getAction().getSensorId(), e.getStatus(), e);
            }
        }
    }
}
