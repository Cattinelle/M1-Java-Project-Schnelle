package com.smarthome.model;

//Fan Implementation:
//Fan speed from 1 to 5. 
//1 is the lowest. 5 is the highest.
//1 used 1/5 of the power rating.
public class SmartFan extends SmartDevice {

    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 5;

    private int speed;

    public SmartFan(String id, String deviceName, double powerRating) {
        super(id, deviceName, powerRating);
        this.speed = MIN_SPEED;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        if (speed < MIN_SPEED || speed > MAX_SPEED) {
            throw new IllegalArgumentException("Fan speed must be between 1 and 5.");
        }
        this.speed = speed;
    }

    @Override
    public String getDeviceType() {
        return "Fan";
    }

    @Override
    public double getEffectivePower() {
        return getPowerRating() * speed / MAX_SPEED;
    }

    @Override
    public String getDeviceInfo() {
        return super.getDeviceInfo() + " - Speed: " + speed + "/" + MAX_SPEED;
    }
}
