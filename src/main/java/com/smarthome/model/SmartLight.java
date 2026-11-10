package com.smarthome.model;

//SmartLight Implementation:
//Brightness from 0 to 100%.
//0 is off. 100 is full brightness.
public class SmartLight extends SmartDevice {

    public static final int MIN_BRIGHTNESS = 0;
    public static final int MAX_BRIGHTNESS = 100;

    private int brightness; // in percent

    public SmartLight(String id, String deviceName, double powerRating) {
        super(id, deviceName, powerRating);
        this.brightness = MAX_BRIGHTNESS;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        if (brightness < MIN_BRIGHTNESS || brightness > MAX_BRIGHTNESS) {
            throw new IllegalArgumentException("Brightness must be between 0 and 100%.");
        }
        this.brightness = brightness;
    }

    @Override
    public String getDeviceType() {
        return "Light";
    }

    @Override
    public double getEffectivePower() {
        return getPowerRating() * brightness / MAX_BRIGHTNESS;
    }

    @Override
    public String getDeviceInfo() {
        return super.getDeviceInfo() + " - Brightness: " + brightness + "%";
    }
}
