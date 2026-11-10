package com.smarthome.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThermostatTest {

    @Test
    void defaultTargetIsTwentyOneDegrees() {
        assertEquals(21.0, new Thermostat("T1", "Heater", 1500).getTargetTemperature());
    }

    @Test
    void targetTemperatureCanBeChanged() {
        Thermostat thermostat = new Thermostat("T1", "Heater", 1500);
        thermostat.setTargetTemperature(10);
        assertEquals(10.0, thermostat.getTargetTemperature());
        thermostat.setTargetTemperature(22.5);
        assertEquals(22.5, thermostat.getTargetTemperature());
    }

    @Test
    void targetTemperatureOutsideRangeIsRejected() {
        Thermostat thermostat = new Thermostat("T1", "Heater", 1500);
        assertThrows(IllegalArgumentException.class, () -> thermostat.setTargetTemperature(9.9));
        assertThrows(IllegalArgumentException.class, () -> thermostat.setTargetTemperature(30.1));
        assertThrows(IllegalArgumentException.class, () -> thermostat.setTargetTemperature(Double.NaN));
        assertEquals(21.0, thermostat.getTargetTemperature());
    }

    @Test
    void consumptionFollowsTargetTemperature() {
        Thermostat thermostat = new Thermostat("T1", "Heater", 1500);
        thermostat.turnOn();
        thermostat.setTargetTemperature(20); // 1500 W x 20/30 = 1000 W

        assertEquals(1000.0, thermostat.getEffectivePower(), 1e-9);
        assertEquals(3.0, thermostat.calculateConsumption(3), 1e-9); // 1000 W x 3 h / 1000
    }

    @Test
    void higherTargetUsesMoreEnergy() {
        Thermostat thermostat = new Thermostat("T1", "Heater", 1500);
        thermostat.turnOn();

        thermostat.setTargetTemperature(18);
        double low = thermostat.calculateConsumption(1);
        thermostat.setTargetTemperature(24);
        double high = thermostat.calculateConsumption(1);

        assertTrue(high > low);
    }
}
