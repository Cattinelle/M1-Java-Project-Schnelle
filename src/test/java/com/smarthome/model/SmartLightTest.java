package com.smarthome.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmartLightTest {

    @Test
    void defaultBrightnessIsFull() {
        assertEquals(100, new SmartLight("L1", "Lamp", 60).getBrightness());
    }

    @Test
    void brightnessCanBeChanged() {
        SmartLight light = new SmartLight("L1", "Lamp", 60);
        light.setBrightness(0);
        assertEquals(0, light.getBrightness());
        light.setBrightness(75);
        assertEquals(75, light.getBrightness());
    }

    @Test
    void brightnessOutsideRangeIsRejected() {
        SmartLight light = new SmartLight("L1", "Lamp", 60);
        assertThrows(IllegalArgumentException.class, () -> light.setBrightness(-1));
        assertThrows(IllegalArgumentException.class, () -> light.setBrightness(101));
        assertEquals(100, light.getBrightness());
    }

    @Test
    void consumptionFollowsBrightness() {
        SmartLight light = new SmartLight("L1", "Lamp", 60);
        light.turnOn();
        light.setBrightness(50); // 30 W

        assertEquals(30.0, light.getEffectivePower(), 1e-9);
        assertEquals(0.3, light.calculateConsumption(10), 1e-9); // 30 W x 10 h / 1000
    }
}
