package com.smarthome.service;

import com.smarthome.model.SmartDevice;
import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//SmartHomeController:
//Manages all registered devices.
//Methods return false when the device is missing (or is the wrong type).
//Invalid property values are rejected by the device classes themselves.
public class SmartHomeController {

    private final List<SmartDevice> devices = new ArrayList<>();

    // Device registration

    public boolean addDevice(SmartDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Device must not be null.");
        }
        if (findDevice(device.getId()).isPresent()) {
            return false; // duplicate ID
        }
        devices.add(device);
        return true;
    }

    public boolean removeDevice(String id) {
        Optional<SmartDevice> device = findDevice(id);
        device.ifPresent(devices::remove);
        return device.isPresent();
    }

    // Lookup

    public Optional<SmartDevice> findDevice(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (SmartDevice device : devices) {
            if (device.getId().equalsIgnoreCase(id.trim())) { // IDs are not case-sensitive
                return Optional.of(device);
            }
        }
        return Optional.empty();
    }

    public List<SmartDevice> getAllDevices() {
        return List.copyOf(devices); // read-only copy, so the list cannot be changed from outside
    }

    // Switching

    public boolean turnOnDevice(String id) {
        Optional<SmartDevice> device = findDevice(id);
        device.ifPresent(SmartDevice::turnOn);
        return device.isPresent();
    }

    public boolean turnOffDevice(String id) {
        Optional<SmartDevice> device = findDevice(id);
        device.ifPresent(SmartDevice::turnOff);
        return device.isPresent();
    }

    // Device-specific settings

    public boolean setLightBrightness(String id, int brightness) {
        Optional<SmartDevice> device = findDevice(id);
        if (device.isPresent() && device.get() instanceof SmartLight light) {
            light.setBrightness(brightness);
            return true;
        }
        return false;
    }

    public boolean setFanSpeed(String id, int speed) {
        Optional<SmartDevice> device = findDevice(id);
        if (device.isPresent() && device.get() instanceof SmartFan fan) {
            fan.setSpeed(speed);
            return true;
        }
        return false;
    }

    public boolean setThermostatTemperature(String id, double targetTemperature) {
        Optional<SmartDevice> device = findDevice(id);
        if (device.isPresent() && device.get() instanceof Thermostat thermostat) {
            thermostat.setTargetTemperature(targetTemperature);
            return true;
        }
        return false;
    }
}
