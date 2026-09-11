package ru.practicum.telemetry.mapper;

import com.google.protobuf.Timestamp;
import org.apache.avro.specific.SpecificRecordBase;
import ru.practicum.telemetry.hubs.*;
import ru.practicum.telemetry.sensors.*;
import ru.practicum.telemetry.sensors.SensorEvent;
import ru.practicum.telemetry.sensors.events.*;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

public class SensorMapper {
    public static SpecificRecordBase toAvro(SensorEvent sensorEvent) {
        if (sensorEvent.getType().equals(SensorEventType.CLIMATE_SENSOR_EVENT)) {
            return mapClimateSensor((ClimateSensorEvent) sensorEvent);
        } else if (sensorEvent.getType().equals(SensorEventType.LIGHT_SENSOR_EVENT)) {
            return mapLightSensor((LightSensorEvent) sensorEvent);
        } else if (sensorEvent.getType().equals(SensorEventType.MOTION_SENSOR_EVENT)) {
            return mapMotionSensor((MotionSensorEvent) sensorEvent);
        } else if (sensorEvent.getType().equals(SensorEventType.SWITCH_SENSOR_EVENT)) {
            return mapSwitchSensor((SwitchSensorEvent) sensorEvent);
        } else if (sensorEvent.getType().equals(SensorEventType.TEMPERATURE_SENSOR_EVENT)) {
            return mapTemperatureSensor((TemperatureSensorEvent) sensorEvent);
        }

        throw new IllegalArgumentException("Unknown type" + sensorEvent.getClass());
    }

    private static SpecificRecordBase mapClimateSensor(ClimateSensorEvent e) {
        var payload = new ClimateSensorAvro();
        payload.setTemperatureC(e.getTemperatureC());
        payload.setHumidity(e.getHumidity());
        payload.setCo2Level(e.getCo2Level());
        return setSensorEventAvro(e, payload);
    }

    private static SpecificRecordBase mapLightSensor(LightSensorEvent e) {
        var payload = new LightSensorAvro();
        payload.setLinkQuality(e.getLinkQuality());
        payload.setLuminosity(e.getLuminosity());
        return setSensorEventAvro(e, payload);
    }

    private static SpecificRecordBase mapTemperatureSensor(TemperatureSensorEvent e) {
        var payload = new TemperatureSensorAvro();
        payload.setTemperatureC(e.getTemperatureC());
        payload.setTemperatureF(e.getTemperatureF());
        return setSensorEventAvro(e, payload);
    }

    private static SpecificRecordBase mapMotionSensor(MotionSensorEvent e) {
        var payload = new MotionSensorAvro();
        payload.setLinkQuality(e.getLinkQuality());
        payload.setMotion(e.isMotion());
        payload.setVoltage(e.getVoltage());
        return setSensorEventAvro(e, payload);
    }

    private static <T extends SpecificRecordBase> SpecificRecordBase setSensorEventAvro(SensorEvent sensorEvent, T payload) {
        var sensorEventAvro = new SensorEventAvro();
        sensorEventAvro.setId(sensorEvent.getId());
        sensorEventAvro.setHubId(sensorEvent.getHubId());
        sensorEventAvro.setTimestamp(sensorEvent.getTimestamp());
        sensorEventAvro.setPayload(payload);
        return sensorEventAvro;
    }

    private static SpecificRecordBase mapSwitchSensor(SwitchSensorEvent e) {
        var payload = new SwitchSensorAvro();
        payload.setState(e.isState());
        return setSensorEventAvro(e, payload);
    }

    private static void setSensorEventFields(SensorEvent sensorEvent, SensorEventProto sensorEventProto) {
        Timestamp timestamp = sensorEventProto.getTimestamp();
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());

        sensorEvent.setId(sensorEventProto.getId());
        sensorEvent.setHubId(sensorEventProto.getHubId());
        sensorEvent.setTimestamp(instant);
    }

    public static SensorEvent toSensorEvent(SensorEventProto sensorEventProto) {
        SensorEventProto.PayloadCase payloadCase = sensorEventProto.getPayloadCase();

        switch (payloadCase) {
            case TEMPERATURE_SENSOR:
                TemperatureSensorProto temperatureSensorProto = sensorEventProto.getTemperatureSensor();
                TemperatureSensorEvent temperatureSensorEvent = new TemperatureSensorEvent();
                setSensorEventFields(temperatureSensorEvent, sensorEventProto);
                temperatureSensorEvent.setTemperatureC(temperatureSensorProto.getTemperatureC());
                temperatureSensorEvent.setTemperatureF(temperatureSensorProto.getTemperatureF());
                return temperatureSensorEvent;
            case SWITCH_SENSOR:
                SwitchSensorProto switchSensorProto = sensorEventProto.getSwitchSensor();
                SwitchSensorEvent switchSensorEvent = new SwitchSensorEvent();
                setSensorEventFields(switchSensorEvent, sensorEventProto);
                switchSensorEvent.setState(switchSensorProto.getState());
                return switchSensorEvent;
            case MOTION_SENSOR:
                MotionSensorProto motionSensorProto = sensorEventProto.getMotionSensor();
                MotionSensorEvent motionSensorEvent = new MotionSensorEvent();
                setSensorEventFields(motionSensorEvent, sensorEventProto);
                motionSensorEvent.setLinkQuality(motionSensorProto.getLinkQuality());
                motionSensorEvent.setMotion(motionSensorProto.getMotion());
                motionSensorEvent.setVoltage(motionSensorProto.getVoltage());
                return motionSensorEvent;
            case LIGHT_SENSOR:
                LightSensorProto lightSensorProto = sensorEventProto.getLightSensor();
                LightSensorEvent lightSensorEvent = new LightSensorEvent();
                setSensorEventFields(lightSensorEvent, sensorEventProto);
                lightSensorEvent.setLinkQuality(lightSensorProto.getLinkQuality());
                lightSensorEvent.setLuminosity(lightSensorProto.getLuminosity());
                return lightSensorEvent;
            case CLIMATE_SENSOR:
                ClimateSensorProto climateSensorProto = sensorEventProto.getClimateSensor();
                ClimateSensorEvent climateSensorEvent = new ClimateSensorEvent();
                setSensorEventFields(climateSensorEvent, sensorEventProto);
                climateSensorEvent.setTemperatureC(climateSensorProto.getTemperatureC());
                climateSensorEvent.setHumidity(climateSensorProto.getHumidity());
                climateSensorEvent.setCo2Level(climateSensorProto.getCo2Level());
                return climateSensorEvent;
            default:
                throw new IllegalArgumentException("unknown SENSOR " + sensorEventProto.getClass());
        }
    }

    public static SensorEvent mapToTemperatureSensorEvent(SensorEventProto event) {
        TemperatureSensorProto temperatureSensorProto = event.getTemperatureSensor();
        TemperatureSensorEvent temperatureSensorEvent = new TemperatureSensorEvent();
        setSensorEventFields(temperatureSensorEvent, event);
        temperatureSensorEvent.setTemperatureC(temperatureSensorProto.getTemperatureC());
        temperatureSensorEvent.setTemperatureF(temperatureSensorProto.getTemperatureF());
        return temperatureSensorEvent;
    }

    public static SensorEvent mapToClimateSensorEvent(SensorEventProto event) {
        ClimateSensorProto climateSensorProto = event.getClimateSensor();
        ClimateSensorEvent climateSensorEvent = new ClimateSensorEvent();
        setSensorEventFields(climateSensorEvent, event);
        climateSensorEvent.setTemperatureC(climateSensorProto.getTemperatureC());
        climateSensorEvent.setHumidity(climateSensorProto.getHumidity());
        climateSensorEvent.setCo2Level(climateSensorProto.getCo2Level());
        return climateSensorEvent;
    }

    public static SensorEvent mapToLightSensorEvent(SensorEventProto event) {
        LightSensorProto lightSensorProto = event.getLightSensor();
        LightSensorEvent lightSensorEvent = new LightSensorEvent();
        setSensorEventFields(lightSensorEvent, event);
        lightSensorEvent.setLinkQuality(lightSensorProto.getLinkQuality());
        lightSensorEvent.setLuminosity(lightSensorProto.getLuminosity());
        return lightSensorEvent;
    }

    public static SensorEvent mapToMotionSensorEvent(SensorEventProto event) {
        MotionSensorProto motionSensorProto = event.getMotionSensor();
        MotionSensorEvent motionSensorEvent = new MotionSensorEvent();
        setSensorEventFields(motionSensorEvent, event);
        motionSensorEvent.setLinkQuality(motionSensorProto.getLinkQuality());
        motionSensorEvent.setMotion(motionSensorProto.getMotion());
        motionSensorEvent.setVoltage(motionSensorProto.getVoltage());
        return motionSensorEvent;
    }

    public static SensorEvent mapToSwitchSensorEvent(SensorEventProto event) {
        SwitchSensorProto switchSensorProto = event.getSwitchSensor();
        SwitchSensorEvent switchSensorEvent = new SwitchSensorEvent();
        setSensorEventFields(switchSensorEvent, event);
        switchSensorEvent.setState(switchSensorProto.getState());
        return switchSensorEvent;
    }

}