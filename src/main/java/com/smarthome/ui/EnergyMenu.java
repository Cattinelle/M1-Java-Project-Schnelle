package com.smarthome.ui;

import com.smarthome.model.SmartDevice;
import com.smarthome.service.EnergyCalculator;
import com.smarthome.service.SmartHomeController;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

//EnergyMenu:
//Collects operating hours and shows consumption and cost using EnergyCalculator.
//Operating hours are kept in memory for the current session only.
public class EnergyMenu {

    private final SmartHomeController controller;
    private final EnergyCalculator calculator;
    private final InputHandler input;
    private final PrintStream out;

    private final Map<SmartDevice, Double> operatingHours = new HashMap<>(); // hours per device
    private boolean hoursEntered = false;

    public EnergyMenu(SmartHomeController controller, EnergyCalculator calculator,
                      InputHandler input, PrintStream out) {
        this.controller = controller;
        this.calculator = calculator;
        this.input = input;
        this.out = out;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            printMenu();
            switch (input.readIntInRange("Select an option: ", 1, 3)) {
                case 1 -> calculateConsumption();
                case 2 -> estimateCost();
                default -> inMenu = false;
            }
        }
    }

    private void printMenu() {
        out.println();
        out.println("========== ENERGY MANAGEMENT ==========");
        out.println();
        out.println("1. Calculate total energy consumption (kWh)");
        out.println("2. Estimate electricity cost (€)");
        out.println("3. Return to main menu");
        out.println();
    }

    // Energy consumption

    private void calculateConsumption() {
        List<SmartDevice> devices = controller.getAllDevices();
        if (devices.isEmpty()) {
            out.println("No devices are registered yet. Add a device first.");
            return;
        }

        out.println("Enter the operating hours for each device. Press ENTER if the device did not run.");
        operatingHours.clear();
        for (SmartDevice device : devices) {
            askHours(device).ifPresent(hours -> operatingHours.put(device, hours));
        }
        hoursEntered = true;

        out.println();
        for (SmartDevice device : devices) {
            Double hours = operatingHours.get(device);
            double kwh = hours == null ? 0.0 : calculator.calculateDeviceConsumption(device, hours);
            out.println(String.format(Locale.ROOT, "%-6s %-25s %8s %10.3f kWh%s",
                    device.getId(), device.getDeviceName(),
                    hours == null ? "-" : String.format(Locale.ROOT, "%.1f h", hours),
                    kwh, device.isOn() ? "" : "  (OFF)"));
        }
        double total = calculator.calculateTotalConsumption(controller, operatingHours);
        out.println(String.format(Locale.ROOT, "Total energy consumption: %.3f kWh", total));
    }

    private Optional<Double> askHours(SmartDevice device) {
        String prompt = String.format("  %s - %s (%s): ",
                device.getId(), device.getDeviceName(), device.isOn() ? "ON" : "OFF");
        while (true) {
            Optional<Double> hours = input.readOptionalDouble(prompt);
            if (hours.isEmpty()) {
                return hours; // did not run
            }
            try {
                calculator.calculateDeviceConsumption(device, hours.get()); // reuses the hours validation
                return hours;
            } catch (IllegalArgumentException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    // Electricity cost

    private void estimateCost() {
        if (!hoursEntered) {
            out.println("No operating hours have been entered yet.");
            out.println("Please choose option 1 (Calculate total energy consumption) first.");
            return;
        }
        out.println("Using the operating hours entered earlier in this session.");
        double total = calculator.calculateTotalConsumption(controller, operatingHours);
        out.println(calculator.getCostSummary(total)); // tariff comes from EnergyCalculator.TARIFF_PER_KWH
    }
}
