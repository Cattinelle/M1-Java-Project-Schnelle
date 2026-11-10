package com.smarthome;

import com.smarthome.repository.DeviceRepository;
import com.smarthome.service.EnergyCalculator;
import com.smarthome.service.SmartHomeController;
import com.smarthome.ui.ConsoleMenu;
import com.smarthome.ui.InputHandler;

import java.io.IOException;
import java.util.Scanner;

//SmartHomeApp:
//Entry point. Creates the objects, loads the saved devices and starts the menu.
public class SmartHomeApp {

    public static void main(String[] args) {
        SmartHomeController controller = new SmartHomeController();
        DeviceRepository repository = new DeviceRepository(); // data/devices.csv

        try {
            int loaded = repository.loadInto(controller);
            System.out.println(loaded + " device(s) loaded from " + repository.getFilePath() + ".");
        } catch (IOException e) {
            System.out.println("Error: devices could not be loaded (" + e.getMessage() + ").");
            System.out.println("Please fix or remove the file and start the application again.");
            return; // stop here so the file is not overwritten by an empty device list
        }

        InputHandler input = new InputHandler(new Scanner(System.in), System.out);
        new ConsoleMenu(controller, repository, new EnergyCalculator(), input, System.out).run();
    }
}
