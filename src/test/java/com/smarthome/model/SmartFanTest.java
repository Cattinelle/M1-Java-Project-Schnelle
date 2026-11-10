package com.smarthome.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmartFanTest {

    @Test
    void defaultSpeedIsLowest() {
        assertEquals(1, new SmartFan("F1", "Fan", 50).getSpeed());
    }

    @Test
    void speedCanBeChanged() {
        SmartFan fan = new SmartFan("F1", "Fan", 50);
        fan.setSpeed(5);
        assertEquals(5, fan.getSpeed());
    }

    @Test
    void speedOutsideRangeIsRejected() {
        SmartFan fan = new SmartFan("F1", "Fan", 50);
        assertThrows(IllegalArgumentException.class, () -> fan.setSpeed(0));
        assertThrows(IllegalArgumentException.class, () -> fan.setSpeed(6));
        assertEquals(1, fan.getSpeed());
    }

    @Test
    void consumptionFollowsSpeed() {
        SmartFan fan = new SmartFan("F1", "Fan", 50);
        fan.turnOn();
        fan.setSpeed(3); // 50 W x 3/5 = 30 W

        assertEquals(30.0, fan.getEffectivePower(), 1e-9);
        assertEquals(0.12, fan.calculateConsumption(4), 1e-9); // 30 W x 4 h / 1000
    }
}
