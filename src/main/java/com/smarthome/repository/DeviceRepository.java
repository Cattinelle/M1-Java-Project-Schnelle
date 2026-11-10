package com.smarthome.repository;

import com.smarthome.model.SmartDevice;
import com.smarthome.model.SmartFan;
import com.smarthome.model.SmartLight;
import com.smarthome.model.Thermostat;
import com.smarthome.service.SmartHomeController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

//DeviceRepository:
//Loads and saves devices in one CSV file.
//Invalid records are skipped when loading; file access errors are passed to the caller.
public class DeviceRepository {

    public static final String HEADER = "id,name,type,powerRating,isOn,brightness,speed,targetTemperature";
    private static final int COLUMN_COUNT = 8;

    private final Path filePath;

    public DeviceRepository() {
        this(Path.of("data", "devices.csv"));
    }

    public DeviceRepository(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path must not be null.");
        }
        this.filePath = filePath;
    }

    public Path getFilePath() {
        return filePath;
    }

    // Loading

    public int loadInto(SmartHomeController controller) throws IOException {
        requireController(controller);
        if (Files.notExists(filePath)) {
            createParentDirectory();
            Files.writeString(filePath, HEADER + System.lineSeparator());
            return 0;
        }

        List<String> lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            return 0; // empty file, nothing to load
        }
        if (!lines.get(0).trim().equals(HEADER)) {
            throw new IOException("Invalid CSV header in " + filePath + ". Expected: " + HEADER);
        }

        int loaded = 0;
        for (int i = 1; i < lines.size(); i++) { // line 0 is the header
            Optional<SmartDevice> device = parseLine(lines.get(i));
            if (device.isPresent() && controller.addDevice(device.get())) { // addDevice rejects duplicate IDs
                loaded++;
            }
        }
        return loaded;
    }

    // Saving

    public void save(SmartHomeController controller) throws IOException {
        requireController(controller);
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (SmartDevice device : controller.getAllDevices()) {
            lines.add(toCsvLine(device));
        }
        createParentDirectory();
        Files.write(filePath, lines); // overwrites the previous content
    }

    // CSV conversion

    private String toCsvLine(SmartDevice device) {
        String type;
        String brightness = "";
        String speed = "";
        String temperature = "";

        if (device instanceof SmartLight light) {
            type = "LIGHT";
            brightness = String.valueOf(light.getBrightness());
        } else if (device instanceof SmartFan fan) {
            type = "FAN";
            speed = String.valueOf(fan.getSpeed());
        } else if (device instanceof Thermostat thermostat) {
            type = "THERMOSTAT";
            temperature = String.valueOf(thermostat.getTargetTemperature());
        } else {
            throw new IllegalStateException("Unsupported device type: " + device.getDeviceType());
        }

        String name = device.getDeviceName().replace(",", " "); // a comma would break the columns
        return String.join(",", device.getId(), name, type, String.valueOf(device.getPowerRating()),
                String.valueOf(device.isOn()), brightness, speed, temperature);
    }

    private Optional<SmartDevice> parseLine(String line) {
        String[] fields = line.split(",", -1); // -1 keeps empty trailing columns
        if (fields.length != COLUMN_COUNT) {
            return Optional.empty();
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
        }

        String id = fields[0];
        String name = fields[1];
        String type = fields[2].toUpperCase(Locale.ROOT); // Locale.ROOT gives the same result on every system
        String isOn = fields[4].toLowerCase(Locale.ROOT);
        if (!isOn.equals("true") && !isOn.equals("false")) {
            return Optional.empty();
        }

        try {
            double powerRating = Double.parseDouble(fields[3]);
            SmartDevice device;
            switch (type) {
                case "LIGHT" -> {
                    SmartLight light = new SmartLight(id, name, powerRating);
                    light.setBrightness(Integer.parseInt(fields[5]));
                    device = light;
                }
                case "FAN" -> {
                    SmartFan fan = new SmartFan(id, name, powerRating);
                    fan.setSpeed(Integer.parseInt(fields[6]));
                    device = fan;
                }
                case "THERMOSTAT" -> {
                    Thermostat thermostat = new Thermostat(id, name, powerRating);
                    thermostat.setTargetTemperature(Double.parseDouble(fields[7]));
                    device = thermostat;
                }
                default -> {
                    return Optional.empty(); // unknown device type
                }
            }
            if (isOn.equals("true")) {
                device.turnOn();
            } else {
                device.turnOff();
            }
            return Optional.of(device);
        } catch (IllegalArgumentException e) { // also covers NumberFormatException
            return Optional.empty(); // missing or invalid value, rejected by the device classes
        }
    }

    // Helpers

    private void requireController(SmartHomeController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("Controller must not be null.");
        }
    }

    private void createParentDirectory() throws IOException {
        Path parent = filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
