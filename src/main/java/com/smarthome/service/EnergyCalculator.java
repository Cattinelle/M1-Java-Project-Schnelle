package com.smarthome.service;

import com.smarthome.model.SmartDevice;

import java.util.Locale;
import java.util.Map;

//EnergyCalculator:
//Calculates device and household energy use (kWh) and the estimated electricity cost.
//The energy formula itself stays in SmartDevice.calculateConsumption().
public class EnergyCalculator {

    public static final double TARIFF_PER_KWH = 0.25; // in euros, change here only

    // Energy consumption

    public double calculateDeviceConsumption(SmartDevice device, double hours) {
        if (device == null) {
            throw new IllegalArgumentException("Device must not be null.");
        }
        return device.calculateConsumption(hours); // validates hours, returns 0 when OFF
    }

    public double calculateTotalConsumption(SmartHomeController controller, Map<SmartDevice, Double> operatingHours) {
        if (controller == null || operatingHours == null) {
            throw new IllegalArgumentException("Controller and operating hours must not be null.");
        }
        double total = 0.0;
        for (SmartDevice device : controller.getAllDevices()) {
            Double hours = operatingHours.get(device);
            if (hours != null) { // no entry means the device did not run
                total += calculateDeviceConsumption(device, hours);
            }
        }
        return total;
    }

    // Electricity cost

    public double calculateCost(double totalKwh) {
        if (!Double.isFinite(totalKwh) || totalKwh < 0) {
            throw new IllegalArgumentException("Energy consumption must be zero or a positive number.");
        }
        return totalKwh * TARIFF_PER_KWH;
    }

    public String getCostSummary(double totalKwh) {
        double cost = calculateCost(totalKwh); // validates totalKwh
        return String.format(Locale.ROOT, // Locale.ROOT always uses a dot for decimals
                "Electricity tariff: €%s/kWh%nTotal energy consumption: %.3f kWh%nEstimated electricity cost: €%.2f",
                TARIFF_PER_KWH, totalKwh, cost);
    }
}
