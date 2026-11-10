package com.smarthome.ui;

import com.smarthome.model.SmartDevice;
import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;
import com.smarthome.repository.DeviceRepository;
import com.smarthome.service.SmartHomeController;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Optional;

//DeviceManagementMenu:
//Adds, removes, switches and modifies devices through SmartHomeController.
//Saves the devices after every successful change.
public class DeviceManagementMenu {

    private final SmartHomeController controller;
    private final DeviceRepository repository;
    private final InputHandler input;
    private final PrintStream out;

    public DeviceManagementMenu(SmartHomeController controller, DeviceRepository repository,
                                InputHandler input, PrintStream out) {
        this.controller = controller;
        this.repository = repository;
        this.input = input;
        this.out = out;
    }

    public void run() {
        boolean inMenu = true;
        while (inMenu) {
            printMenu();
            switch (input.readIntInRange("Select an option: ", 1, 5)) {
                case 1 -> addDevice();
                case 2 -> removeDevice();
                case 3 -> switchDevice();
                case 4 -> modifySettings();
                default -> inMenu = false;
            }
        }
    }

    private void printMenu() {
        out.println();
        out.println("========== DEVICE MANAGEMENT ==========");
        out.println();
        out.println("1. Add a new device");
        out.println("2. Remove an existing device");
        out.println("3. Turn a device ON or OFF");
        out.println("4. Modify device settings");
        out.println("5. Return to main menu");
        out.println();
    }

    // Add

    private void addDevice() {
        out.println();
        out.println("Device types: 1. SmartLight  2. SmartFan  3. Thermostat  4. Cancel");
        int type = input.readIntInRange("Select a device type: ", 1, 4);
        if (type == 4) {
            return;
        }

        String id = input.readRequiredText("Device ID (e.g. L2): ");
        if (controller.findDevice(id).isPresent()) {
            out.println("Error: a device with ID '" + id + "' already exists. Device not added.");
            return;
        }
        String name = input.readRequiredText("Device name: ");
        if (name.contains(",")) {
            out.println("Note: commas are not allowed in the data file and will be saved as spaces.");
        }
        double powerRating = input.readDouble("Power rating (W): ");

        SmartDevice device;
        try {
            device = switch (type) {
                case 1 -> new SmartLight(id, name, powerRating);
                case 2 -> new SmartFan(id, name, powerRating);
                default -> new Thermostat(id, name, powerRating);
            };
        } catch (IllegalArgumentException e) { // rejected by the SmartDevice constructor
            out.println("Error: " + e.getMessage() + " Device not added.");
            return;
        }

        controller.addDevice(device);
        changeSetting(device);
        if (input.readYesNo("Turn the device ON now? (y/n): ")) {
            controller.turnOnDevice(device.getId());
        }
        out.println("Device added: " + device.getDeviceInfo());
        saveDevices();
    }

    // Remove

    private void removeDevice() {
        Optional<SmartDevice> device = askForDevice();
        if (device.isEmpty()) {
            return;
        }
        if (input.readYesNo("Remove " + device.get().getDeviceInfo() + "? (y/n): ")) {
            controller.removeDevice(device.get().getId());
            out.println("Device '" + device.get().getId() + "' removed.");
            saveDevices();
        } else {
            out.println("Removal cancelled.");
        }
    }

    // Switch ON/OFF

    private void switchDevice() {
        Optional<SmartDevice> device = askForDevice();
        if (device.isEmpty()) {
            return;
        }
        String id = device.get().getId();
        out.println("Current status: " + (device.get().isOn() ? "ON" : "OFF"));
        out.println("1. Turn ON  2. Turn OFF");
        if (input.readIntInRange("Select an option: ", 1, 2) == 1) {
            controller.turnOnDevice(id);
        } else {
            controller.turnOffDevice(id);
        }
        out.println("Updated status: " + device.get().getDeviceInfo());
        saveDevices();
    }

    // Modify settings

    private void modifySettings() {
        Optional<SmartDevice> device = askForDevice();
        if (device.isEmpty()) {
            return;
        }
        out.println("Current settings: " + device.get().getDeviceInfo());
        changeSetting(device.get());
        out.println("Updated settings: " + device.get().getDeviceInfo());
        saveDevices();
    }

    //Asks for the device-specific value until the device accepts it.
    //The range checks are done by the device setters, not here.
    private void changeSetting(SmartDevice device) {
        String id = device.getId();
        while (true) {
            try {
                if (device instanceof SmartLight light) {
                    controller.setLightBrightness(id, input.readInt(String.format(Locale.ROOT,
                            "Brightness (%d-%d%%, current %d%%): ",
                            SmartLight.MIN_BRIGHTNESS, SmartLight.MAX_BRIGHTNESS, light.getBrightness())));
                } else if (device instanceof SmartFan fan) {
                    controller.setFanSpeed(id, input.readInt(String.format(Locale.ROOT,
                            "Fan speed (%d-%d, current %d): ",
                            SmartFan.MIN_SPEED, SmartFan.MAX_SPEED, fan.getSpeed())));
                } else if (device instanceof Thermostat thermostat) {
                    controller.setThermostatTemperature(id, input.readDouble(String.format(Locale.ROOT,
                            "Target temperature (%.0f-%.0f °C, current %.1f °C): ",
                            Thermostat.MIN_TEMPERATURE, Thermostat.MAX_TEMPERATURE,
                            thermostat.getTargetTemperature())));
                }
                return;
            } catch (IllegalArgumentException e) { // value rejected by the device
                out.println("Error: " + e.getMessage());
            }
        }
    }

    // Helpers

    private Optional<SmartDevice> askForDevice() {
        if (controller.getAllDevices().isEmpty()) {
            out.println("No devices are registered yet. Add a device first.");
            return Optional.empty();
        }
        String id = input.readLine("Device ID: ");
        if (id.isEmpty()) {
            out.println("Error: no device ID entered.");
            return Optional.empty();
        }
        Optional<SmartDevice> device = controller.findDevice(id);
        if (device.isEmpty()) {
            out.println("Error: no device found with ID '" + id + "'.");
        }
        return device;
    }

    private void saveDevices() {
        try {
            repository.save(controller);
            out.println("Changes saved.");
        } catch (IOException e) {
            out.println("Error: changes could not be saved to " + repository.getFilePath()
                    + " (" + e.getMessage() + ").");
        }
    }
}
