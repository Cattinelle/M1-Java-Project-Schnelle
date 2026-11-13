package com.smarthome.model;

import com.smarthome.interfaces.EnergyConsumable;
import com.smarthome.interfaces.Switchable;

import java.util.regex.Pattern;

public abstract class SmartDevice implements Switchable, EnergyConsumable {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");

    private final String id;
    private String deviceName;
    private final double powerRating; // in watts
    private boolean on;

    protected SmartDevice(String id, String deviceName, double powerRating) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Device ID must not be empty.");
        }
        if (!ID_PATTERN.matcher(id.trim()).matches()) { // also keeps commas out of the CSV file
            throw new IllegalArgumentException(
                    "Device ID may only contain letters, numbers, hyphens (-) and underscores (_).");
        }
        if (!Double.isFinite(powerRating) || powerRating <= 0) {
            throw new IllegalArgumentException("Power rating must be a positive number of watts.");
        }
        this.id = id.trim();
        setDeviceName(deviceName);
        this.powerRating = powerRating;
        this.on = false; // every device starts switched off
    }


    public abstract String getDeviceType();

    public abstract double getEffectivePower();

    //Switchable interface implementation

    @Override
    public void turnOn() {
        on = true;
    }

    @Override
    public void turnOff() {
        on = false;
    }

    @Override
    public boolean isOn() {
        return on;
    }

    //EnergyConsumable interface implementation

    @Override
    public final double calculateConsumption(double hours) {
        if (!Double.isFinite(hours) || hours < 0) {
            throw new IllegalArgumentException("Operating hours must be zero or a positive number.");
        }
        if (!on) {
            return 0.0;
        }
        return getEffectivePower() * hours / 1000.0;
    }


    // Getters and setters

    public String getId() {
        return id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException("Device name must not be empty.");
        }
        this.deviceName = deviceName.trim();
    }

    public double getPowerRating() {
        return powerRating;
    }

    //Device info display
    public String getDeviceInfo() {
        return String.format("[%s] %s (%s) - %s - %.1f W",
                id, deviceName, getDeviceType(), on ? "ON" : "OFF", powerRating);
    }

    @Override
    public String toString() {
        return getDeviceInfo();
    }
}
