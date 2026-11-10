package com.smarthome.service;

import com.smarthome.model.SmartDevice;
import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnergyCalculatorTest {

    private static final double DELTA = 1e-9;

    private EnergyCalculator calculator;
    private SmartHomeController controller;
    private SmartLight light;
    private SmartFan fan;
    private Thermostat thermostat;
    private Map<SmartDevice, Double> operatingHours;

    @BeforeEach
    void setUp() {
        calculator = new EnergyCalculator();
        controller = new SmartHomeController();

        light = new SmartLight("L1", "Desk Lamp", 60);
        light.setBrightness(50);               // 30 W
        fan = new SmartFan("F1", "Ceiling Fan", 50);
        fan.setSpeed(3);                       // 30 W
        thermostat = new Thermostat("T1", "Living Room Heater", 1500);
        thermostat.setTargetTemperature(20);   // 1000 W

        controller.addDevice(light);
        controller.addDevice(fan);
        controller.addDevice(thermostat);
        light.turnOn();
        fan.turnOn();
        thermostat.turnOn();

        operatingHours = new HashMap<>();
        operatingHours.put(light, 5.0);
        operatingHours.put(fan, 3.0);
        operatingHours.put(thermostat, 8.0);
    }

    // Individual devices

    @Test
    void deviceConsumptionUsesPowerAndHours() {
        assertEquals(0.15, calculator.calculateDeviceConsumption(light, 5), DELTA); // 30 W x 5 h / 1000
    }

    @Test
    void switchedOffDeviceConsumesNothing() {
        light.turnOff();
        assertEquals(0.0, calculator.calculateDeviceConsumption(light, 5));
    }

    @Test
    void zeroHoursGivesZeroConsumption() {
        assertEquals(0.0, calculator.calculateDeviceConsumption(thermostat, 0));
    }

    @Test
    void invalidHoursAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateDeviceConsumption(fan, -1));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateDeviceConsumption(fan, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateDeviceConsumption(fan, Double.POSITIVE_INFINITY));
    }

    @Test
    void nullDeviceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateDeviceConsumption(null, 5));
    }

    // Device-specific settings

    @Test
    void lightBrightnessAffectsConsumption() {
        light.setBrightness(100); // 60 W
        assertEquals(0.3, calculator.calculateDeviceConsumption(light, 5), DELTA);
        light.setBrightness(25);  // 15 W
        assertEquals(0.075, calculator.calculateDeviceConsumption(light, 5), DELTA);
    }

    @Test
    void fanSpeedAffectsConsumption() {
        fan.setSpeed(1); // 10 W
        assertEquals(0.03, calculator.calculateDeviceConsumption(fan, 3), DELTA);
        fan.setSpeed(5); // 50 W
        assertEquals(0.15, calculator.calculateDeviceConsumption(fan, 3), DELTA);
    }

    @Test
    void thermostatTemperatureAffectsConsumption() {
        thermostat.setTargetTemperature(15); // 750 W
        assertEquals(6.0, calculator.calculateDeviceConsumption(thermostat, 8), DELTA);
        thermostat.setTargetTemperature(30); // 1500 W
        assertEquals(12.0, calculator.calculateDeviceConsumption(thermostat, 8), DELTA);
    }

    // Total consumption

    @Test
    void totalConsumptionUsesEachDevicesOwnHours() {
        // light 30 W x 5 h + fan 30 W x 3 h + thermostat 1000 W x 8 h = 0.15 + 0.09 + 8.0 kWh
        assertEquals(8.24, calculator.calculateTotalConsumption(controller, operatingHours), DELTA);
    }

    @Test
    void totalConsumptionChangesWhenOneDevicesHoursChange() {
        operatingHours.put(fan, 10.0); // 30 W x 10 h = 0.3 kWh instead of 0.09
        assertEquals(8.45, calculator.calculateTotalConsumption(controller, operatingHours), DELTA);
    }

    @Test
    void totalConsumptionExcludesSwitchedOffDevices() {
        thermostat.turnOff();
        assertEquals(0.24, calculator.calculateTotalConsumption(controller, operatingHours), DELTA);
    }

    @Test
    void totalConsumptionIsZeroWhenAllDevicesAreOff() {
        light.turnOff();
        fan.turnOff();
        thermostat.turnOff();
        assertEquals(0.0, calculator.calculateTotalConsumption(controller, operatingHours));
    }

    @Test
    void totalConsumptionIsZeroForEmptyController() {
        assertEquals(0.0, calculator.calculateTotalConsumption(new SmartHomeController(), operatingHours));
    }

    @Test
    void totalConsumptionIsZeroForEmptyHoursMap() {
        assertEquals(0.0, calculator.calculateTotalConsumption(controller, new HashMap<>()));
    }

    @Test
    void deviceWithoutHoursEntryCountsAsNotRunning() {
        operatingHours.remove(thermostat);
        assertEquals(0.24, calculator.calculateTotalConsumption(controller, operatingHours), DELTA);
    }

    @Test
    void hoursForUnregisteredDeviceAreIgnored() {
        SmartLight unregistered = new SmartLight("L9", "Garage Lamp", 100);
        unregistered.turnOn();
        operatingHours.put(unregistered, 10.0);

        assertEquals(8.24, calculator.calculateTotalConsumption(controller, operatingHours), DELTA);
    }

    @Test
    void totalConsumptionRejectsInvalidHours() {
        operatingHours.put(fan, -2.0);
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateTotalConsumption(controller, operatingHours));
    }

    @Test
    void totalConsumptionRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateTotalConsumption(null, operatingHours));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateTotalConsumption(controller, null));
    }

    // Electricity cost

    @Test
    void tariffIsTwentyFiveCentsPerKwh() {
        assertEquals(0.25, EnergyCalculator.TARIFF_PER_KWH);
    }

    @Test
    void costIsConsumptionTimesTariff() {
        double totalKwh = calculator.calculateTotalConsumption(controller, operatingHours); // 8.24 kWh
        assertEquals(2.06, calculator.calculateCost(totalKwh), DELTA);                      // 8.24 x 0.25
    }

    @Test
    void costIsZeroForZeroConsumption() {
        assertEquals(0.0, calculator.calculateCost(0));
    }

    @Test
    void costRejectsInvalidConsumption() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateCost(-1));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateCost(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateCost(Double.POSITIVE_INFINITY));
    }

    // Cost summary

    @Test
    void costSummaryShowsTariffBeforeConsumptionAndCost() {
        String[] lines = calculator.getCostSummary(8.24).split("\\R");

        assertEquals(3, lines.length);
        assertEquals("Electricity tariff: €0.25/kWh", lines[0]);
        assertEquals("Total energy consumption: 8.240 kWh", lines[1]);
        assertEquals("Estimated electricity cost: €2.06", lines[2]);
    }

    @Test
    void costSummaryUsesTheDisplayedTariff() {
        double totalKwh = 12.5;
        String[] lines = calculator.getCostSummary(totalKwh).split("\\R");

        // read the tariff back from the text and check the shown cost was calculated with it
        double shownTariff = Double.parseDouble(lines[0].replaceAll("[^0-9.]", ""));
        double shownCost = Double.parseDouble(lines[2].replaceAll("[^0-9.]", ""));

        assertEquals(EnergyCalculator.TARIFF_PER_KWH, shownTariff);
        assertEquals(totalKwh * shownTariff, shownCost, 0.005); // cost is shown rounded to cents
    }

    @Test
    void costSummaryRejectsInvalidConsumption() {
        assertThrows(IllegalArgumentException.class, () -> calculator.getCostSummary(-5));
    }
}
