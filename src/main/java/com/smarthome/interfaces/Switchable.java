package com.smarthome.interfaces;

/**
 * Capability of anything that can be switched on and off.
 *
 * <p>Kept deliberately small (Interface Segregation Principle): a class that only
 * needs to toggle power does not have to know anything about energy calculations.</p>
 */
public interface Switchable {

    /** Switches the device on. Calling it on a device that is already on has no effect. */
    void turnOn();

    /** Switches the device off. Calling it on a device that is already off has no effect. */
    void turnOff();

    /** @return {@code true} if the device is currently switched on */
    boolean isOn();
}
