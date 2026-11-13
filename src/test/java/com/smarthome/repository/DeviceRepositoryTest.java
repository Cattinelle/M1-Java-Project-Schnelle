package com.smarthome.repository;

import com.smarthome.model.SmartDevice;
import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;
import com.smarthome.service.SmartHomeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRepositoryTest {

    @TempDir
    Path tempDir; // a fresh folder for every test, so data/devices.csv is never touched

    private Path csvFile;
    private DeviceRepository repository;
    private SmartHomeController controller;

    @BeforeEach
    void setUp() {
        csvFile = tempDir.resolve("devices.csv");
        repository = new DeviceRepository(csvFile);
        controller = new SmartHomeController();
    }

    private void writeCsv(String... records) throws IOException {
        StringBuilder content = new StringBuilder(DeviceRepository.HEADER).append("\n");
        for (String record : records) {
            content.append(record).append("\n");
        }
        Files.writeString(csvFile, content.toString());
    }

    // Saving

    @Test
    void saveWritesHeaderAndOneLinePerDevice() throws IOException {
        SmartLight light = new SmartLight("L1", "Desk Lamp", 60);
        light.setBrightness(75);
        light.turnOn();
        SmartFan fan = new SmartFan("F1", "Ceiling Fan", 50);
        fan.setSpeed(3);
        Thermostat thermostat = new Thermostat("T1", "Living Room Heater", 1500);
        thermostat.setTargetTemperature(22.5);
        thermostat.turnOn();
        controller.addDevice(light);
        controller.addDevice(fan);
        controller.addDevice(thermostat);

        repository.save(controller);

        assertEquals(List.of(
                DeviceRepository.HEADER,
                "L1,Desk Lamp,LIGHT,60.0,true,75,,",
                "F1,Ceiling Fan,FAN,50.0,false,,3,",
                "T1,Living Room Heater,THERMOSTAT,1500.0,true,,,22.5"
        ), Files.readAllLines(csvFile));
    }

    @Test
    void saveWithNoDevicesWritesOnlyHeader() throws IOException {
        repository.save(controller);
        assertEquals(List.of(DeviceRepository.HEADER), Files.readAllLines(csvFile));
    }

    @Test
    void saveCreatesMissingDirectory() throws IOException {
        Path nestedFile = tempDir.resolve("data").resolve("devices.csv");
        controller.addDevice(new SmartFan("F1", "Fan", 50));

        new DeviceRepository(nestedFile).save(controller);

        assertTrue(Files.exists(nestedFile));
    }

    @Test
    void saveReplacesCommasInNames() throws IOException {
        controller.addDevice(new SmartLight("L1", "Lamp, Kitchen", 60));
        repository.save(controller);

        SmartHomeController reloaded = new SmartHomeController();
        assertEquals(1, repository.loadInto(reloaded));
        assertEquals("Lamp  Kitchen", reloaded.findDevice("L1").orElseThrow().getDeviceName());
    }

    @Test
    void deviceNameWithCommaIsSavedAndReloadedWithSpaces() throws IOException {
        SmartFan fan = new SmartFan("F1", "Fan,Bedroom,Upstairs", 50);
        fan.setSpeed(4);
        fan.turnOn();
        controller.addDevice(fan);

        repository.save(controller);

        assertEquals("F1,Fan Bedroom Upstairs,FAN,50.0,true,,4,", Files.readAllLines(csvFile).get(1));

        SmartHomeController reloaded = new SmartHomeController();
        assertEquals(1, repository.loadInto(reloaded));
        SmartFan reloadedFan = (SmartFan) reloaded.findDevice("F1").orElseThrow();
        assertEquals("Fan Bedroom Upstairs", reloadedFan.getDeviceName());
        assertEquals(4, reloadedFan.getSpeed()); // other columns are not shifted
        assertTrue(reloadedFan.isOn());
    }

    @Test
    void saveRejectsNullController() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    // Loading

    @Test
    void loadRestoresDeviceTypesPropertiesAndStates() throws IOException {
        writeCsv("L1,Desk Lamp,LIGHT,60.0,true,75,,",
                "F1,Ceiling Fan,FAN,50.0,false,,3,",
                "T1,Living Room Heater,THERMOSTAT,1500.0,true,,,22.5");

        assertEquals(3, repository.loadInto(controller));

        SmartLight light = (SmartLight) controller.findDevice("L1").orElseThrow();
        assertEquals("Desk Lamp", light.getDeviceName());
        assertEquals(60.0, light.getPowerRating());
        assertEquals(75, light.getBrightness());
        assertTrue(light.isOn());

        SmartFan fan = (SmartFan) controller.findDevice("F1").orElseThrow();
        assertEquals(3, fan.getSpeed());
        assertFalse(fan.isOn());

        Thermostat thermostat = (Thermostat) controller.findDevice("T1").orElseThrow();
        assertEquals(22.5, thermostat.getTargetTemperature());
        assertTrue(thermostat.isOn());
    }

    @Test
    void savedDevicesCanBeLoadedBack() throws IOException {
        SmartLight light = new SmartLight("L1", "Desk Lamp", 60);
        light.setBrightness(0);
        SmartFan fan = new SmartFan("F1", "Ceiling Fan", 50);
        fan.setSpeed(5);
        fan.turnOn();
        Thermostat thermostat = new Thermostat("T1", "Heater", 1500);
        thermostat.setTargetTemperature(10);
        controller.addDevice(light);
        controller.addDevice(fan);
        controller.addDevice(thermostat);
        repository.save(controller);

        SmartHomeController reloaded = new SmartHomeController();
        assertEquals(3, repository.loadInto(reloaded));

        List<SmartDevice> devices = reloaded.getAllDevices();
        assertInstanceOf(SmartLight.class, devices.get(0));
        assertInstanceOf(SmartFan.class, devices.get(1));
        assertInstanceOf(Thermostat.class, devices.get(2));
        assertEquals(0, ((SmartLight) devices.get(0)).getBrightness());
        assertEquals(5, ((SmartFan) devices.get(1)).getSpeed());
        assertEquals(10.0, ((Thermostat) devices.get(2)).getTargetTemperature());
        assertFalse(devices.get(0).isOn());
        assertTrue(devices.get(1).isOn());
        assertFalse(devices.get(2).isOn());
    }

    @Test
    void loadAcceptsLowerCaseValuesAndExtraSpaces() throws IOException {
        writeCsv(" L1 , Desk Lamp , light , 60 , TRUE , 40 , , ");

        assertEquals(1, repository.loadInto(controller));
        SmartLight light = (SmartLight) controller.findDevice("L1").orElseThrow();
        assertEquals("Desk Lamp", light.getDeviceName());
        assertEquals(40, light.getBrightness());
        assertTrue(light.isOn());
    }

    @Test
    void loadRejectsNullController() {
        assertThrows(IllegalArgumentException.class, () -> repository.loadInto(null));
    }

    // Header validation

    @Test
    void loadRejectsWrongHeader() throws IOException {
        Files.writeString(csvFile, "id,name,type\nL1,Desk Lamp,LIGHT,60.0,true,75,,\n");

        assertThrows(IOException.class, () -> repository.loadInto(controller));
        assertTrue(controller.getAllDevices().isEmpty());
    }

    @Test
    void loadRejectsFileWithoutHeader() throws IOException {
        Files.writeString(csvFile, "L1,Desk Lamp,LIGHT,60.0,true,75,,\n");

        assertThrows(IOException.class, () -> repository.loadInto(controller));
        assertTrue(controller.getAllDevices().isEmpty());
    }

    // Missing and empty files

    @Test
    void loadCreatesMissingFileAndDirectoryWithHeader() throws IOException {
        Path missingFile = tempDir.resolve("data").resolve("devices.csv");

        assertEquals(0, new DeviceRepository(missingFile).loadInto(controller));

        assertTrue(Files.exists(missingFile));
        assertEquals(List.of(DeviceRepository.HEADER), Files.readAllLines(missingFile));
        assertTrue(controller.getAllDevices().isEmpty());
    }

    @Test
    void loadHandlesCompletelyEmptyFile() throws IOException {
        Files.writeString(csvFile, "");

        assertEquals(0, repository.loadInto(controller));
        assertTrue(controller.getAllDevices().isEmpty());
    }

    @Test
    void loadHandlesHeaderOnlyFile() throws IOException {
        writeCsv();

        assertEquals(0, repository.loadInto(controller));
        assertTrue(controller.getAllDevices().isEmpty());
    }

    // Invalid records

    @Test
    void loadSkipsMalformedRecords() throws IOException {
        writeCsv("",                                      // blank line
                "L1,Desk Lamp,LIGHT,60.0,true,75",        // too few columns
                "L2,Lamp,LIGHT,60.0,true,75,,,extra",     // too many columns
                "L3,Lamp,LIGHT,sixty,true,75,,",          // power rating is not a number
                "L4,Lamp,LIGHT,60.0,yes,75,,",            // ON/OFF is not true or false
                "L5,Lamp,LIGHT,60.0,true,,,",             // missing brightness
                "F1,Fan,FAN,50.0,false,,,",               // missing speed
                "T1,Heater,THERMOSTAT,1500.0,true,,,",    // missing temperature
                "P1,Plug,PLUG,10.0,true,,,");             // unknown device type

        assertEquals(0, repository.loadInto(controller));
        assertTrue(controller.getAllDevices().isEmpty());
    }

    @Test
    void loadSkipsRecordsWithInvalidProperties() throws IOException {
        writeCsv("L1,Lamp,LIGHT,60.0,true,150,,",         // brightness above 100
                "F1,Fan,FAN,50.0,false,,9,",              // speed above 5
                "T1,Heater,THERMOSTAT,1500.0,true,,,45",  // temperature above 30
                "T2,Heater,THERMOSTAT,1500.0,true,,,NaN", // temperature is not a real number
                "L2,Lamp,LIGHT,-5,true,50,,",             // negative power rating
                ",Lamp,LIGHT,60.0,true,50,,",             // empty ID
                "L3,,LIGHT,60.0,true,50,,");              // empty name

        assertEquals(0, repository.loadInto(controller));
        assertTrue(controller.getAllDevices().isEmpty());
    }

    @Test
    void loadContinuesWithValidRecordsAfterInvalidOnes() throws IOException {
        writeCsv("L1,Desk Lamp,LIGHT,60.0,true,75,,",
                "this is not a valid record",
                "F1,Fan,FAN,50.0,false,,9,",
                "F2,Ceiling Fan,FAN,50.0,false,,3,",
                "X1,Robot,VACUUM,30.0,true,,,",
                "T1,Heater,THERMOSTAT,1500.0,true,,,22.5");

        assertEquals(3, repository.loadInto(controller));
        assertTrue(controller.findDevice("L1").isPresent());
        assertTrue(controller.findDevice("F2").isPresent());
        assertTrue(controller.findDevice("T1").isPresent());
        assertTrue(controller.findDevice("F1").isEmpty());
    }

    // Duplicate IDs

    @Test
    void loadKeepsFirstRecordWhenIdsAreDuplicated() throws IOException {
        writeCsv("L1,First Lamp,LIGHT,60.0,true,75,,",
                "L1,Second Lamp,LIGHT,40.0,false,20,,",
                "l1,Lowercase Fan,FAN,50.0,false,,3,");  // IDs are not case-sensitive

        assertEquals(1, repository.loadInto(controller));
        assertEquals(1, controller.getAllDevices().size());
        assertEquals("First Lamp", controller.findDevice("L1").orElseThrow().getDeviceName());
    }

    @Test
    void loadDoesNotReplaceAlreadyRegisteredDevice() throws IOException {
        controller.addDevice(new SmartFan("F1", "Existing Fan", 50));
        writeCsv("F1,Ceiling Fan,FAN,50.0,true,,3,",
                "L1,Desk Lamp,LIGHT,60.0,true,75,,");

        assertEquals(1, repository.loadInto(controller));
        assertEquals("Existing Fan", controller.findDevice("F1").orElseThrow().getDeviceName());
        assertEquals(2, controller.getAllDevices().size());
    }

    @Test
    void loadContinuesWithValidRecordsAfterDuplicates() throws IOException {
        writeCsv("L1,Desk Lamp,LIGHT,60.0,true,75,,",
                "L1,Duplicate Lamp,LIGHT,40.0,false,20,,",
                "F1,Ceiling Fan,FAN,50.0,false,,3,",
                "f1,Duplicate Fan,FAN,30.0,true,,5,",
                "T1,Heater,THERMOSTAT,1500.0,true,,,22.5");

        assertEquals(3, repository.loadInto(controller));
        assertEquals("Desk Lamp", controller.findDevice("L1").orElseThrow().getDeviceName());
        assertEquals("Ceiling Fan", controller.findDevice("F1").orElseThrow().getDeviceName());
        assertTrue(controller.findDevice("T1").isPresent());
    }
}
