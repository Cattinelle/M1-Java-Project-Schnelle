package com.smarthome.ui;

import com.smarthome.model.SmartLight;
import com.smarthome.repository.DeviceRepository;
import com.smarthome.service.EnergyCalculator;
import com.smarthome.service.SmartHomeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleMenuTest {

    @TempDir
    Path tempDir; // keeps data/devices.csv untouched

    private SmartHomeController controller;
    private DeviceRepository repository;

    @BeforeEach
    void setUp() {
        controller = new SmartHomeController();
        repository = new DeviceRepository(tempDir.resolve("devices.csv"));
    }

    private String runWithInput(String... lines) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        InputHandler input = new InputHandler(new Scanner(String.join("\n", lines) + "\n"), out);
        new ConsoleMenu(controller, repository, new EnergyCalculator(), input, out).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    // Main menu

    @Test
    void mainMenuShowsTitleAndOptions() {
        String output = runWithInput("5");

        assertTrue(output.contains("SCHNELLE'S SMART HOME ENERGY MANAGER"));
        assertTrue(output.contains("1. View all registered smart devices"));
        assertTrue(output.contains("5. Exit application"));
    }

    @Test
    void invalidMenuChoicesDoNotStopTheApplication() {
        String output = runWithInput("9", "hello", "", "5");

        assertTrue(output.contains("Error: Please enter a number between 1 and 5."));
        assertTrue(output.contains("Error: 'hello' is not a whole number."));
        assertTrue(output.contains("Goodbye!"));
    }

    // View devices

    @Test
    void viewShowsMessageWhenNoDevicesAreRegistered() {
        String output = runWithInput("1", "5");
        assertTrue(output.contains("No devices are registered yet."));
    }

    @Test
    void viewListsDevicesAndShowsSelectedDetails() {
        SmartLight light = new SmartLight("L1", "Desk Lamp", 60);
        light.setBrightness(75);
        light.turnOn();
        controller.addDevice(light);

        String output = runWithInput("1", "l1", "5");

        assertTrue(output.contains("L1     Desk Lamp                 Light       ON           60.0 W"));
        assertTrue(output.contains("--- Device details ---"));
        assertTrue(output.contains("Brightness: 75%"));
        assertTrue(output.contains("Power at current setting: 45.0 W"));
    }

    @Test
    void viewReportsUnknownDeviceId() {
        controller.addDevice(new SmartLight("L1", "Desk Lamp", 60));
        String output = runWithInput("1", "X9", "5");
        assertTrue(output.contains("Error: no device found with ID 'X9'."));
    }

    // Help

    @Test
    void helpExplainsUsageAndShowsTariff() {
        String output = runWithInput("4", "5");

        assertTrue(output.contains("========== HELP =========="));
        assertTrue(output.contains("SmartFan: speed from 1 to 5."));
        assertTrue(output.contains("€" + EnergyCalculator.TARIFF_PER_KWH + "/kWh"));
    }

    // Exit

    @Test
    void exitSavesDevicesAndSaysGoodbye() throws IOException {
        controller.addDevice(new SmartLight("L1", "Desk Lamp", 60));

        String output = runWithInput("5");

        assertTrue(output.contains("Devices saved to"));
        assertTrue(output.endsWith("Goodbye!" + System.lineSeparator()));
        assertTrue(Files.readString(repository.getFilePath()).contains("L1,Desk Lamp,LIGHT"));
    }

    @Test
    void closedInputExitsCleanlyAndStillSaves() {
        controller.addDevice(new SmartLight("L1", "Desk Lamp", 60));

        String output = runWithInput("1"); // input ends while the app waits for more

        assertTrue(output.contains("Input closed."));
        assertTrue(output.contains("Goodbye!"));
        assertTrue(Files.exists(repository.getFilePath()));
    }

    @Test
    void saveErrorOnExitIsReported() throws IOException {
        Path folderInsteadOfFile = Files.createDirectory(tempDir.resolve("blocked.csv"));
        repository = new DeviceRepository(folderInsteadOfFile); // writing to a folder fails

        String output = runWithInput("5");

        assertTrue(output.contains("Error: devices could not be saved"));
        assertTrue(output.contains("Goodbye!"));
    }
}
