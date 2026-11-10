package com.smarthome.interfaces;

public interface Switchable {

    //Turns the device on. Has no effect on a device that is already on.
    void turnOn();

    //Turns the device off. Has no effect on a device that is already off.
    void turnOff();

    //Returns true if the device is on, false otherwise.
    boolean isOn();
}
