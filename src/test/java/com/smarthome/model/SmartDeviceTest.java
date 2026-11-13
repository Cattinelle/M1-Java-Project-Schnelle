package com.smarthome.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the behaviour every device inherits from SmartDevice. */
class SmartDeviceTest {

    // ----- Construction -----

    @Test
    void constructorStoresValuesAndStartsOff() {
        SmartDevice device = new SmartLight("L1", "Desk Lamp", 60);

        assertEquals("L1", device.getId());
        assertEquals("Desk Lamp", device.getDeviceName());
        assertEquals(60, device.getPowerRating());
        assertFalse(device.isOn());
    }

    @Test
    void constructorRejectsEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> new SmartLight(null, "Lamp", 60));
        assertThrows(IllegalArgumentException.class, () -> new SmartLight("", "Lamp", 60));
        assertThrows(IllegalArgumentException.class, () -> new SmartLight("   ", "Lamp", 60));
    }

    @ParameterizedTest
    @ValueSource(strings = {"L1", "FAN02", "LIGHT-01", "ROOM_01"})
    void constructorAcceptsIdsWithLettersNumbersHyphensAndUnderscores(String id) {
        assertEquals(id, new SmartLight(id, "Lamp", 60).getId());
    }

    @Test
    void constructorTrimsSpacesAroundValidId() {
        assertEquals("L1", new SmartLight("  L1  ", "Lamp", 60).getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"K,1", "L 1", "L@1", "L.1", "L1/2"})
    void constructorRejectsIdsWithOtherCharacters(String id) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new SmartLight(id, "Lamp", 60));
        assertEquals("Device ID may only contain letters, numbers, hyphens (-) and underscores (_).",
                e.getMessage());
    }

    @Test
    void constructorRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new SmartFan("F1", null, 50));
        assertThrows(IllegalArgumentException.class, () -> new SmartFan("F1", " ", 50));
    }

    @Test
    void constructorRejectsInvalidPowerRating() {
        assertThrows(IllegalArgumentException.class, () -> new Thermostat("T1", "Heater", 0));
        assertThrows(IllegalArgumentException.class, () -> new Thermostat("T1", "Heater", -100));
        assertThrows(IllegalArgumentException.class, () -> new Thermostat("T1", "Heater", Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new Thermostat("T1", "Heater", Double.POSITIVE_INFINITY));
    }

    // ----- Name update -----

    @Test
    void deviceNameCanBeUpdated() {
        SmartDevice device = new SmartFan("F1", "Fan", 50);
        device.setDeviceName("Bedroom Fan");
        assertEquals("Bedroom Fan", device.getDeviceName());
    }

    @Test
    void deviceNameCannotBeSetToEmpty() {
        SmartDevice device = new SmartFan("F1", "Fan", 50);
        assertThrows(IllegalArgumentException.class, () -> device.setDeviceName(""));
        assertEquals("Fan", device.getDeviceName());
    }

    // ----- Switching -----

    @Test
    void deviceCanBeSwitchedOnAndOff() {
        SmartDevice device = new SmartLight("L1", "Lamp", 60);

        device.turnOn();
        assertTrue(device.isOn());

        device.turnOff();
        assertFalse(device.isOn());
    }

    // ----- Energy consumption -----

    @Test
    void switchedOffDeviceConsumesNothing() {
        SmartDevice device = new SmartLight("L1", "Lamp", 60);
        assertEquals(0.0, device.calculateConsumption(10));
    }

    @Test
    void switchedOnDeviceUsesEnergyFormula() {
        SmartDevice device = new SmartLight("L1", "Lamp", 60); // brightness 100% -> 60 W
        device.turnOn();
        // 60 W x 5 h / 1000 = 0.3 kWh
        assertEquals(0.3, device.calculateConsumption(5), 1e-9);
    }

    @Test
    void zeroHoursGivesZeroConsumption() {
        SmartDevice device = new SmartLight("L1", "Lamp", 60);
        device.turnOn();
        assertEquals(0.0, device.calculateConsumption(0));
    }

    @Test
    void invalidHoursAreRejected() {
        SmartDevice device = new SmartLight("L1", "Lamp", 60);
        device.turnOn();
        assertThrows(IllegalArgumentException.class, () -> device.calculateConsumption(-1));
        assertThrows(IllegalArgumentException.class, () -> device.calculateConsumption(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> device.calculateConsumption(Double.POSITIVE_INFINITY));
    }

    @Test
    void invalidHoursAreRejectedEvenWhenOff() {
        SmartDevice device = new SmartLight("L1", "Lamp", 60);
        assertThrows(IllegalArgumentException.class, () -> device.calculateConsumption(-1));
    }

    // ----- Polymorphism -----

    @Test
    void differentDeviceTypesCanBeHandledThroughTheBaseType() {
        SmartLight light = new SmartLight("L1", "Lamp", 100);
        light.setBrightness(50);   // 50 W
        SmartFan fan = new SmartFan("F1", "Fan", 50);
        fan.setSpeed(5);           // 50 W
        Thermostat thermostat = new Thermostat("T1", "Heater", 2000);
        thermostat.setTargetTemperature(15); // 1000 W

        List<SmartDevice> devices = List.of(light, fan, thermostat);

        double total = 0;
        for (SmartDevice device : devices) {
            device.turnOn();
            total += device.calculateConsumption(2); // each call runs the subclass's own power rule
        }

        // (50 + 50 + 1000) W x 2 h / 1000 = 2.2 kWh
        assertEquals(2.2, total, 1e-9);
        assertEquals("Light", devices.get(0).getDeviceType());
        assertEquals("Fan", devices.get(1).getDeviceType());
        assertEquals("Thermostat", devices.get(2).getDeviceType());
    }

    @Test
    void deviceInfoShowsCommonAndSpecificDetails() {
        SmartDevice light = new SmartLight("L1", "Lamp", 60);
        light.turnOn();

        String info = light.getDeviceInfo();

        assertTrue(info.contains("L1"));
        assertTrue(info.contains("Lamp"));
        assertTrue(info.contains("Light"));
        assertTrue(info.contains("ON"));
        assertTrue(info.contains("Brightness: 100%"));
        assertEquals(info, light.toString());
    }
}
