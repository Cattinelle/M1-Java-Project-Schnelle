package com.smarthome.model;


//Thermostat Implementation:
//Target temperature from 10 to 30 °C.
//Default target temperature is 21 °C.

public class Thermostat extends SmartDevice {

    public static final double MIN_TEMPERATURE = 10.0;
    public static final double MAX_TEMPERATURE = 30.0;
    public static final double DEFAULT_TEMPERATURE = 21.0;

    private double targetTemperature; // in °C

    public Thermostat(String id, String deviceName, double powerRating) {
        super(id, deviceName, powerRating);
        this.targetTemperature = DEFAULT_TEMPERATURE;
    }

    public double getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(double targetTemperature) {
        // The ! (...) form also rejects NaN, which fails every comparison.
        if (!(targetTemperature >= MIN_TEMPERATURE && targetTemperature <= MAX_TEMPERATURE)) {
            throw new IllegalArgumentException("Target temperature must be between 10 and 30 °C.");
        }
        this.targetTemperature = targetTemperature;
    }

    @Override
    public String getDeviceType() {
        return "Thermostat";
    }

    @Override
    public double getEffectivePower() {
        return getPowerRating() * targetTemperature / MAX_TEMPERATURE;
    }

    @Override
    public String getDeviceInfo() {
        return super.getDeviceInfo() + String.format(" - Target: %.1f °C", targetTemperature);
    }
}
