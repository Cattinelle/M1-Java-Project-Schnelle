package com.smarthome.service;

import com.smarthome.exception.DuplicateDeviceException;
import com.smarthome.model.SmartDevice;
import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SmartHomeControllerTest {

    private SmartHomeController controller;
    private SmartLight light;
    private SmartFan fan;
    private Thermostat thermostat;

    @BeforeEach
    void setUp() {
        controller = new SmartHomeController();
        light = new SmartLight("L1", "Desk Lamp", 60);
        fan = new SmartFan("F1", "Ceiling Fan", 50);
        thermostat = new Thermostat("T1", "Living Room Heater", 1500);
        controller.addDevice(light);
        controller.addDevice(fan);
        controller.addDevice(thermostat);
    }

    // Adding devices

    @Test
    void addDeviceRegistersNewDevice() {
        SmartLight lamp = new SmartLight("L2", "Bedside Lamp", 40);

        controller.addDevice(lamp);

        assertEquals(4, controller.getAllDevices().size());
        assertSame(lamp, controller.findDevice("L2").orElseThrow());
    }

    @Test
    void addDeviceThrowsForDuplicateId() {
        DuplicateDeviceException e = assertThrows(DuplicateDeviceException.class,
                () -> controller.addDevice(new SmartFan("L1", "Another Device", 30)));

        assertEquals("A device with ID 'L1' already exists.", e.getMessage());
        assertEquals(3, controller.getAllDevices().size());
        assertSame(light, controller.findDevice("L1").orElseThrow()); // original kept
    }

    @Test
    void addDeviceThrowsForDuplicateIdWithDifferentCase() {
        DuplicateDeviceException e = assertThrows(DuplicateDeviceException.class,
                () -> controller.addDevice(new SmartFan("l1", "Lowercase ID", 30))); // IDs are not case-sensitive

        assertEquals("A device with ID 'l1' already exists.", e.getMessage());
        assertEquals(3, controller.getAllDevices().size());
        assertSame(light, controller.findDevice("L1").orElseThrow());
    }

    @Test
    void addDeviceRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> controller.addDevice(null));
    }

    // Removing devices

    @Test
    void removeDeviceRemovesExistingDevice() {
        assertTrue(controller.removeDevice("F1"));
        assertTrue(controller.findDevice("F1").isEmpty());
        assertEquals(2, controller.getAllDevices().size());
    }

    @Test
    void removedIdCanBeReused() {
        controller.removeDevice("F1");
        assertDoesNotThrow(() -> controller.addDevice(new SmartFan("F1", "New Fan", 40)));
        assertEquals("New Fan", controller.findDevice("F1").orElseThrow().getDeviceName());
    }

    @Test
    void removeDeviceReturnsFalseForUnknownId() {
        assertFalse(controller.removeDevice("X99"));
        assertFalse(controller.removeDevice(null));
        assertEquals(3, controller.getAllDevices().size());
    }

    // Finding devices

    @Test
    void findDeviceReturnsMatchingDevice() {
        Optional<SmartDevice> result = controller.findDevice("T1");

        assertTrue(result.isPresent());
        assertSame(thermostat, result.get());
    }

    @Test
    void findDeviceIgnoresCaseAndSurroundingSpaces() {
        assertSame(light, controller.findDevice(" l1 ").orElseThrow());
    }

    @Test
    void findDeviceReturnsEmptyForUnknownOrNullId() {
        assertTrue(controller.findDevice("X99").isEmpty());
        assertTrue(controller.findDevice("").isEmpty());
        assertTrue(controller.findDevice(null).isEmpty());
    }

    @Test
    void findDeviceReturnsEmptyForWhitespaceOnlyId() {
        Optional<SmartDevice> result = assertDoesNotThrow(() -> controller.findDevice("   "));
        assertTrue(result.isEmpty());
    }

    // Retrieving all devices

    @Test
    void getAllDevicesReturnsDevicesInInsertionOrder() {
        assertEquals(List.of(light, fan, thermostat), controller.getAllDevices());
    }

    @Test
    void getAllDevicesIsEmptyForNewController() {
        assertTrue(new SmartHomeController().getAllDevices().isEmpty());
    }

    @Test
    void getAllDevicesCannotBeModifiedFromOutside() {
        List<SmartDevice> devices = controller.getAllDevices();
        assertThrows(UnsupportedOperationException.class, () -> devices.clear());
        assertEquals(3, controller.getAllDevices().size());
    }

    // Switching devices

    @Test
    void turnOnAndOffSwitchesDevice() {
        assertTrue(controller.turnOnDevice("F1"));
        assertTrue(fan.isOn());

        assertTrue(controller.turnOffDevice("F1"));
        assertFalse(fan.isOn());
    }

    @Test
    void switchingWorksForEveryDeviceType() {
        for (SmartDevice device : controller.getAllDevices()) {
            assertTrue(controller.turnOnDevice(device.getId()));
            assertTrue(device.isOn());
        }
    }

    @Test
    void switchingUnknownDeviceReturnsFalse() {
        assertFalse(controller.turnOnDevice("X99"));
        assertFalse(controller.turnOffDevice("X99"));
    }

    // Updating device-specific settings

    @Test
    void setLightBrightnessUpdatesLight() {
        assertTrue(controller.setLightBrightness("L1", 40));
        assertEquals(40, light.getBrightness());
    }

    @Test
    void setFanSpeedUpdatesFan() {
        assertTrue(controller.setFanSpeed("F1", 4));
        assertEquals(4, fan.getSpeed());
    }

    @Test
    void setThermostatTemperatureUpdatesThermostat() {
        assertTrue(controller.setThermostatTemperature("T1", 23.5));
        assertEquals(23.5, thermostat.getTargetTemperature());
    }

    @Test
    void boundaryValuesAreAccepted() {
        assertTrue(controller.setLightBrightness("L1", 0));
        assertTrue(controller.setLightBrightness("L1", 100));
        assertTrue(controller.setFanSpeed("F1", 1));
        assertTrue(controller.setFanSpeed("F1", 5));
        assertTrue(controller.setThermostatTemperature("T1", 10));
        assertTrue(controller.setThermostatTemperature("T1", 30));
    }

    // Invalid property values

    @Test
    void invalidValuesAreRejectedAndSettingsUnchanged() {
        assertThrows(IllegalArgumentException.class, () -> controller.setLightBrightness("L1", 101));
        assertThrows(IllegalArgumentException.class, () -> controller.setFanSpeed("F1", 0));
        assertThrows(IllegalArgumentException.class, () -> controller.setThermostatTemperature("T1", 35));

        assertEquals(100, light.getBrightness());
        assertEquals(1, fan.getSpeed());
        assertEquals(21.0, thermostat.getTargetTemperature());
    }

    @Test
    void nonFiniteThermostatTemperaturesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.setThermostatTemperature("T1", Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> controller.setThermostatTemperature("T1", Double.POSITIVE_INFINITY));

        assertEquals(21.0, thermostat.getTargetTemperature());
    }

    // Operations on nonexistent or wrong-type devices

    @Test
    void settingsForUnknownDeviceReturnFalse() {
        assertFalse(controller.setLightBrightness("X99", 50));
        assertFalse(controller.setFanSpeed("X99", 3));
        assertFalse(controller.setThermostatTemperature("X99", 20));
    }

    @Test
    void settingsForWrongDeviceTypeReturnFalse() {
        assertFalse(controller.setLightBrightness("F1", 50));      // F1 is a fan
        assertFalse(controller.setFanSpeed("T1", 3));              // T1 is a thermostat
        assertFalse(controller.setThermostatTemperature("L1", 20)); // L1 is a light
    }
}
