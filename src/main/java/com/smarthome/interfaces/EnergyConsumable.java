package com.smarthome.interfaces;

public interface EnergyConsumable {

    //Calcs energy consumption in kWh based on how long (in hours) the device has been on. 
    //Returns 0 if the device is off.
    double calculateConsumption(double hours);
}
