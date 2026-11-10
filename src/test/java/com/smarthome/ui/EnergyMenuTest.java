package com.smarthome.ui;

import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;
import com.smarthome.service.EnergyCalculator;
import com.smarthome.service.SmartHomeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class EnergyMenuTest {

    private SmartHomeController controller;
    private Thermostat thermostat;

    @BeforeEach
    void setUp() {
        controller = new SmartHomeController();

        SmartLight light = new SmartLight("L1", "Desk Lamp", 60);
        light.setBrightness(50);               // 30 W
        light.turnOn();
        SmartFan fan = new SmartFan("F1", "Ceiling Fan", 50);
        fan.setSpeed(3);                       // 30 W
        fan.turnOn();
        thermostat = new Thermostat("T1", "Living Room Heater", 1500);
        thermostat.setTargetTemperature(20);   // 1000 W
        thermostat.turnOn();

        controller.addDevice(light);
        controller.addDevice(fan);
        controller.addDevice(thermostat);
    }

    private String runWithInput(String... lines) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        InputHandler input = new InputHandler(new Scanner(String.join("\n", lines) + "\n"), out);
        new EnergyMenu(controller, new EnergyCalculator(), input, out).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private int countOccurrences(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    // Energy consumption

    @Test
    void consumptionUsesEachDevicesOwnHours() {
        String output = runWithInput("1", "5", "3", "8", "3");

        // 30 W x 5 h + 30 W x 3 h + 1000 W x 8 h = 0.15 + 0.09 + 8.0 kWh
        assertTrue(output.contains("Total energy consumption: 8.240 kWh"));
        assertTrue(output.contains("0.150 kWh"));
        assertTrue(output.contains("8.000 kWh"));
    }

    @Test
    void enterMeansTheDeviceDidNotRun() {
        String output = runWithInput("1", "5", "", "", "3");
        assertTrue(output.contains("Total energy consumption: 0.150 kWh"));
    }

    @Test
    void switchedOffDevicesAreExcluded() {
        thermostat.turnOff();
        String output = runWithInput("1", "5", "3", "8", "3");

        assertTrue(output.contains("Total energy consumption: 0.240 kWh"));
        assertTrue(output.contains("(OFF)"));
    }

    @Test
    void invalidHoursAreRejectedAndAskedAgain() {
        String output = runWithInput("1", "-2", "five", "5", "3", "8", "3");

        assertTrue(output.contains("Error: Operating hours must be zero or a positive number."));
        assertTrue(output.contains("Error: 'five' is not a valid number."));
        assertTrue(output.contains("Total energy consumption: 8.240 kWh"));
    }

    @Test
    void consumptionReportsWhenNoDevicesAreRegistered() {
        controller = new SmartHomeController();
        String output = runWithInput("1", "3");
        assertTrue(output.contains("No devices are registered yet."));
    }

    // Electricity cost

    @Test
    void costBeforeEnteringHoursAsksUserToCalculateFirst() {
        String output = runWithInput("2", "3");

        assertTrue(output.contains("No operating hours have been entered yet."));
        assertFalse(output.contains("Estimated electricity cost"));
    }

    @Test
    void costShowsTariffConsumptionAndCostInOrder() {
        String output = runWithInput("1", "5", "3", "8", "2", "3");

        int tariff = output.indexOf("Electricity tariff: €0.25/kWh");
        int consumption = output.lastIndexOf("Total energy consumption: 8.240 kWh");
        int cost = output.indexOf("Estimated electricity cost: €2.06");
        assertTrue(tariff >= 0 && consumption > tariff && cost > consumption);
    }

    @Test
    void costReusesHoursWithoutAskingAgain() {
        String output = runWithInput("1", "5", "3", "8", "2", "2", "3");

        assertEquals(1, countOccurrences(output, "L1 - Desk Lamp")); // hours asked only once
        assertEquals(2, countOccurrences(output, "Estimated electricity cost: €2.06"));
    }

    @Test
    void costUsesTariffFromEnergyCalculator() {
        String output = runWithInput("1", "", "", "10", "2", "3"); // only the heater runs: 10 kWh

        assertTrue(output.contains("Electricity tariff: €" + EnergyCalculator.TARIFF_PER_KWH + "/kWh"));
        assertTrue(output.contains(String.format(java.util.Locale.ROOT,
                "Estimated electricity cost: €%.2f", 10 * EnergyCalculator.TARIFF_PER_KWH)));
    }

    @Test
    void newHoursReplaceTheOldOnes() {
        String output = runWithInput("1", "5", "3", "8", "1", "", "", "4", "2", "3");
        assertTrue(output.contains("Estimated electricity cost: €1.00")); // 4 kWh x 0.25
    }
}
