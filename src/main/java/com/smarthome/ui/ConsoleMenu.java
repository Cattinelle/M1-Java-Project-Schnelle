package com.smarthome.ui;

import com.smarthome.model.SmartDevice;
import com.smarthome.repository.DeviceRepository;
import com.smarthome.service.EnergyCalculator;
import com.smarthome.service.SmartHomeController;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

//ConsoleMenu:
//Shows the main menu and passes each choice to the right part of the application.
//Device management and energy monitoring have their own menu classes.
public class ConsoleMenu {

    private final SmartHomeController controller;
    private final DeviceRepository repository;
    private final InputHandler input;
    private final PrintStream out;
    private final DeviceManagementMenu deviceMenu;
    private final EnergyMenu energyMenu;

    public ConsoleMenu(SmartHomeController controller, DeviceRepository repository,
                       EnergyCalculator calculator, InputHandler input, PrintStream out) {
        this.controller = controller;
        this.repository = repository;
        this.input = input;
        this.out = out;
        this.deviceMenu = new DeviceManagementMenu(controller, repository, input, out);
        this.energyMenu = new EnergyMenu(controller, calculator, input, out);
    }

    public void run() {
        try {
            boolean running = true;
            while (running) {
                printMainMenu();
                switch (input.readIntInRange("Select an option: ", 1, 5)) {
                    case 1 -> viewDevices();
                    case 2 -> deviceMenu.run();
                    case 3 -> energyMenu.run();
                    case 4 -> showHelp();
                    default -> running = false;
                }
            }
        } catch (NoSuchElementException e) { // input stream closed (e.g. Ctrl+Z / Ctrl+D)
            out.println();
            out.println("Input closed.");
        }
        exitApplication();
    }

    private void printMainMenu() {
        out.println();
        out.println("========================================");
        out.println(" SCHNELLE'S SMART HOME ENERGY MANAGER");
        out.println("========================================");
        out.println();
        out.println("1. View all registered smart devices");
        out.println("2. Manage devices (add, remove, switch or modify)");
        out.println("3. Monitor energy consumption and electricity costs");
        out.println("4. Help and usage instructions");
        out.println("5. Exit application");
        out.println();
    }

    // View devices

    private void viewDevices() {
        List<SmartDevice> devices = controller.getAllDevices();
        out.println();
        if (devices.isEmpty()) {
            out.println("No devices are registered yet. Use option 2 to add a device.");
            return;
        }

        out.println(String.format("%-6s %-25s %-11s %-6s %12s", "ID", "Name", "Type", "Status", "Power"));
        out.println("-".repeat(64));
        for (SmartDevice device : devices) {
            out.println(String.format(Locale.ROOT, "%-6s %-25s %-11s %-6s %10.1f W",
                    device.getId(), device.getDeviceName(), device.getDeviceType(),
                    device.isOn() ? "ON" : "OFF", device.getPowerRating()));
        }

        out.println();
        String id = input.readLine("Enter a device ID to see its details (or press ENTER to return): ");
        if (id.isEmpty()) {
            return;
        }
        Optional<SmartDevice> device = controller.findDevice(id);
        if (device.isPresent()) {
            out.println();
            out.println("--- Device details ---");
            out.println(device.get().getDeviceInfo()); // each device type adds its own settings
            out.println(String.format(Locale.ROOT, "Power at current setting: %.1f W",
                    device.get().getEffectivePower()));
        } else {
            out.println("Error: no device found with ID '" + id + "'.");
        }
    }

    // Help

    private void showHelp() {
        out.println();
        out.println("========== HELP ==========");
        out.println("Navigation: type the number of a menu option and press ENTER.");
        out.println("            Invalid choices are rejected and you can try again.");
        out.println();
        out.println("Managing devices (option 2):");
        out.println("  - Add a SmartLight, SmartFan or Thermostat with a unique ID, a name and its power in watts.");
        out.println("  - Remove a device by its ID (you are asked to confirm).");
        out.println("  - Turn a device ON or OFF by its ID.");
        out.println("  - Every change is saved automatically to " + repository.getFilePath() + ".");
        out.println();
        out.println("Device settings (option 2, then 4):");
        out.println("  - SmartLight: brightness from 0 to 100%.");
        out.println("  - SmartFan: speed from 1 to 5.");
        out.println("  - Thermostat: target temperature from 10 to 30 °C.");
        out.println();
        out.println("Energy consumption (option 3, then 1):");
        out.println("  - Enter how many hours each device ran. Press ENTER if it did not run.");
        out.println("  - Energy (kWh) = power at current setting (W) x hours / 1000.");
        out.println("  - Devices that are OFF use 0 kWh.");
        out.println();
        out.println("Electricity cost (option 3, then 2):");
        out.println("  - Uses the hours entered in option 1 during this session.");
        out.println("  - Cost = total kWh x fixed tariff of €" + EnergyCalculator.TARIFF_PER_KWH + "/kWh.");
    }

    // Exit

    private void exitApplication() {
        try {
            repository.save(controller); // safeguard, changes are already saved after each update
            out.println("Devices saved to " + repository.getFilePath() + ".");
        } catch (IOException e) {
            out.println("Error: devices could not be saved (" + e.getMessage() + ").");
        }
        out.println("Goodbye!");
    }
}
