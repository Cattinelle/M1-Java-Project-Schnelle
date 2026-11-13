package com.smarthome.ui;

import com.smarthome.model.SmartDevice;
import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;
import com.smarthome.repository.DeviceRepository;
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

class DeviceManagementMenuTest {

    @TempDir
    Path tempDir;

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
        new DeviceManagementMenu(controller, repository, input, out).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private SmartHomeController reloadFromFile() throws IOException {
        SmartHomeController reloaded = new SmartHomeController(); // simulates restarting the app
        repository.loadInto(reloaded);
        return reloaded;
    }

    // Add

    @Test
    void addLightRegistersAndSavesDevice() throws IOException {
        String output = runWithInput("1", "1", "L2", "Bedside Lamp", "40", "80", "y", "5");

        SmartLight light = (SmartLight) controller.findDevice("L2").orElseThrow();
        assertEquals("Bedside Lamp", light.getDeviceName());
        assertEquals(80, light.getBrightness());
        assertTrue(light.isOn());
        assertTrue(output.contains("Device added:"));
        assertTrue(output.contains("Changes saved."));

        SmartLight reloaded = (SmartLight) reloadFromFile().findDevice("L2").orElseThrow();
        assertEquals(80, reloaded.getBrightness());
        assertTrue(reloaded.isOn());
    }

    @Test
    void addFanAndThermostatUseTheirOwnSettings() {
        runWithInput("1", "2", "F2", "Desk Fan", "35", "4", "n",
                "1", "3", "T2", "Bedroom Heater", "1200", "19,5", "n", "5");

        assertEquals(4, ((SmartFan) controller.findDevice("F2").orElseThrow()).getSpeed());
        Thermostat thermostat = (Thermostat) controller.findDevice("T2").orElseThrow();
        assertEquals(19.5, thermostat.getTargetTemperature());
        assertFalse(thermostat.isOn());
    }

    @Test
    void addAsksAgainWhenSettingIsOutOfRange() {
        String output = runWithInput("1", "1", "L2", "Lamp", "40", "150", "-1", "50", "n", "5");

        assertTrue(output.contains("Error: Brightness must be between 0 and 100%."));
        assertEquals(50, ((SmartLight) controller.findDevice("L2").orElseThrow()).getBrightness());
    }

    @Test
    void addRejectsDuplicateId() {
        controller.addDevice(new SmartFan("F1", "Ceiling Fan", 50));

        String output = runWithInput("1", "1", "f1", "5");

        assertTrue(output.contains("Error: a device with ID 'f1' already exists. Device not added."));
        assertFalse(output.contains("Device added:"));
        assertEquals(1, controller.getAllDevices().size());
        assertInstanceOf(SmartFan.class, controller.findDevice("F1").orElseThrow());
        assertFalse(Files.exists(repository.getFilePath())); // nothing saved
    }

    @Test
    void addRejectsInvalidPowerRating() {
        String output = runWithInput("1", "2", "F2", "Fan", "abc", "-50", "5");

        assertTrue(output.contains("Error: 'abc' is not a valid number."));
        assertTrue(output.contains("Error: Power rating must be a positive number of watts. Device not added."));
        assertTrue(controller.getAllDevices().isEmpty());
        assertFalse(Files.exists(repository.getFilePath())); // nothing changed, nothing saved
    }

    @Test
    void addRejectsInvalidIdAndWritesNothing() {
        String output = runWithInput("1", "1", "K,1", "Kitchen Lamp", "40", "5");

        assertTrue(output.contains("Error: Device ID may only contain letters, numbers, hyphens (-) and underscores (_)."
                + " Device not added."));
        assertTrue(controller.getAllDevices().isEmpty());
        assertFalse(Files.exists(repository.getFilePath())); // no corrupted record written
    }

    @Test
    void addCanBeCancelled() {
        runWithInput("1", "4", "5");
        assertTrue(controller.getAllDevices().isEmpty());
    }

    @Test
    void addWarnsAboutCommasInName() {
        String output = runWithInput("1", "1", "L2", "Lamp, Kitchen", "40", "50", "n", "5");
        assertTrue(output.contains("Note: commas are not allowed"));
    }

    // Remove

    @Test
    void removeAfterConfirmationDeletesAndSaves() throws IOException {
        controller.addDevice(new SmartFan("F1", "Ceiling Fan", 50));
        controller.addDevice(new SmartLight("L1", "Desk Lamp", 60));

        String output = runWithInput("2", "F1", "y", "5");

        assertTrue(output.contains("Device 'F1' removed."));
        assertTrue(controller.findDevice("F1").isEmpty());
        assertTrue(reloadFromFile().findDevice("F1").isEmpty());
        assertTrue(reloadFromFile().findDevice("L1").isPresent());
    }

    @Test
    void removeCanBeCancelled() {
        controller.addDevice(new SmartFan("F1", "Ceiling Fan", 50));

        String output = runWithInput("2", "F1", "n", "5");

        assertTrue(output.contains("Removal cancelled."));
        assertTrue(controller.findDevice("F1").isPresent());
    }

    @Test
    void removeReportsUnknownAndEmptyIds() {
        controller.addDevice(new SmartFan("F1", "Ceiling Fan", 50));

        String output = runWithInput("2", "X9", "2", "", "5");

        assertTrue(output.contains("Error: no device found with ID 'X9'."));
        assertTrue(output.contains("Error: no device ID entered."));
        assertEquals(1, controller.getAllDevices().size());
    }

    @Test
    void actionsReportWhenNoDevicesAreRegistered() {
        String output = runWithInput("2", "3", "4", "5");
        assertEquals(3, output.split("No devices are registered yet", -1).length - 1);
    }

    // Switch ON/OFF

    @Test
    void switchTurnsDeviceOnAndOffAndSaves() throws IOException {
        controller.addDevice(new SmartLight("L1", "Desk Lamp", 60));

        String output = runWithInput("3", "L1", "1", "5");
        assertTrue(controller.findDevice("L1").orElseThrow().isOn());
        assertTrue(output.contains("Updated status: [L1] Desk Lamp (Light) - ON"));
        assertTrue(reloadFromFile().findDevice("L1").orElseThrow().isOn());

        runWithInput("3", "L1", "2", "5");
        assertFalse(controller.findDevice("L1").orElseThrow().isOn());
        assertFalse(reloadFromFile().findDevice("L1").orElseThrow().isOn());
    }

    // Modify settings

    @Test
    void modifyChangesTheRightSettingForEachType() throws IOException {
        controller.addDevice(new SmartLight("L1", "Desk Lamp", 60));
        controller.addDevice(new SmartFan("F1", "Ceiling Fan", 50));
        controller.addDevice(new Thermostat("T1", "Heater", 1500));

        String output = runWithInput("4", "L1", "30", "4", "F1", "2", "4", "T1", "24.5", "5");

        assertTrue(output.contains("Brightness (0-100%, current 100%)"));
        assertTrue(output.contains("Fan speed (1-5, current 1)"));
        assertTrue(output.contains("Target temperature (10-30 °C, current 21.0 °C)"));

        SmartHomeController reloaded = reloadFromFile();
        assertEquals(30, ((SmartLight) reloaded.findDevice("L1").orElseThrow()).getBrightness());
        assertEquals(2, ((SmartFan) reloaded.findDevice("F1").orElseThrow()).getSpeed());
        assertEquals(24.5, ((Thermostat) reloaded.findDevice("T1").orElseThrow()).getTargetTemperature());
    }

    @Test
    void modifyAsksAgainForInvalidValues() {
        controller.addDevice(new SmartFan("F1", "Ceiling Fan", 50));
        controller.addDevice(new Thermostat("T1", "Heater", 1500));

        String output = runWithInput("4", "F1", "0", "fast", "3", "4", "T1", "35", "NaN", "18", "5");

        assertTrue(output.contains("Error: Fan speed must be between 1 and 5."));
        assertTrue(output.contains("Error: 'fast' is not a whole number."));
        assertTrue(output.contains("Error: Target temperature must be between 10 and 30 °C."));
        assertTrue(output.contains("Error: 'NaN' is not a valid number."));
        assertEquals(3, ((SmartFan) controller.findDevice("F1").orElseThrow()).getSpeed());
        assertEquals(18.0, ((Thermostat) controller.findDevice("T1").orElseThrow()).getTargetTemperature());
    }

    @Test
    void modifyReportsUnknownDevice() {
        controller.addDevice(new SmartFan("F1", "Ceiling Fan", 50));
        String output = runWithInput("4", "Z1", "5");
        assertTrue(output.contains("Error: no device found with ID 'Z1'."));
    }

    // Save errors

    @Test
    void saveErrorIsReportedButChangeStaysInMemory() throws IOException {
        Path folderInsteadOfFile = Files.createDirectory(tempDir.resolve("blocked.csv"));
        repository = new DeviceRepository(folderInsteadOfFile); // writing to a folder fails
        controller.addDevice(new SmartLight("L1", "Desk Lamp", 60));

        String output = runWithInput("3", "L1", "1", "5");

        assertTrue(output.contains("Error: changes could not be saved"));
        SmartDevice light = controller.findDevice("L1").orElseThrow();
        assertTrue(light.isOn());
    }
}
