package com.smarthome.model;

import com.smarthome.exception.InvalidEnergyValueException;
import com.smarthome.interfaces.EnergyConsumable;
import com.smarthome.interfaces.Switchable;

import java.util.Locale;
import java.util.Objects;

/**
 * Abstract base class for every smart device in the household.
 *
 * <p>Holds the state shared by all devices (id, name, rated power, on/off) and implements
 * the {@link Switchable} and {@link EnergyConsumable} capabilities once, so subclasses do
 * not duplicate them.</p>
 *
 * <p>{@link #calculateConsumption(double)} is a <em>template method</em>: it is {@code final}
 * so that the guarantees promised by the {@link EnergyConsumable} contract (input validation,
 * zero consumption when off, non-negative result) hold for every subclass (Liskov Substitution
 * Principle). Subclasses customise behaviour only through the hooks
 * {@link #getEffectivePowerWatts()} and {@link #getBillableHours(double)}.</p>
 */
public abstract class SmartDevice implements Switchable, EnergyConsumable {

    /** Conversion factor used in: Energy (kWh) = Power (W) x Time (h) / 1000. */
    protected static final double WATTS_PER_KILOWATT = 1000.0;

    private final String id;
    private final String name;
    private final double powerRating;
    private boolean on;

    /**
     * @param id          unique, non-blank identifier (e.g. "D001")
     * @param name        non-blank human-readable name
     * @param powerRating rated (maximum) power in watts, must be positive and finite
     * @throws IllegalArgumentException     if id or name is blank
     * @throws InvalidEnergyValueException  if the power rating is not a positive number
     */
    protected SmartDevice(String id, String name, double powerRating) {
        this.id = requireText(id, "Device id");
        this.name = requireText(name, "Device name");
        if (!Double.isFinite(powerRating) || powerRating <= 0) {
            throw new InvalidEnergyValueException(
                    "Power rating must be a positive number of watts, but was " + powerRating + ".");
        }
        this.powerRating = powerRating;
        this.on = false; // every device starts switched off
    }

    // ---------------------------------------------------------------- Switchable

    @Override
    public void turnOn() {
        on = true;
    }

    @Override
    public void turnOff() {
        on = false;
    }

    @Override
    public boolean isOn() {
        return on;
    }

    // ---------------------------------------------------------- EnergyConsumable

    @Override
    public final double calculateConsumption(double hours) {
        if (!Double.isFinite(hours) || hours < 0) {
            throw new InvalidEnergyValueException(
                    "Operating hours must be zero or a positive number, but was " + hours + ".");
        }
        if (!on) {
            return 0.0; // a switched-off device never records active consumption
        }
        return getEffectivePowerWatts() * getBillableHours(hours) / WATTS_PER_KILOWATT;
    }

    /**
     * Power drawn in watts with the device's current settings (e.g. brightness, fan speed),
     * independent of whether the device is on. Default: the rated power.
     */
    public double getEffectivePowerWatts() {
        return powerRating;
    }

    /**
     * Hook for devices that limit how long they actually run (e.g. an auto-off timer).
     * Default: the device runs for the full requested time.
     */
    protected double getBillableHours(double requestedHours) {
        return requestedHours;
    }

    // ------------------------------------------------------- device-specific API

    /** @return the type of this device, used for display and CSV persistence */
    public abstract DeviceType getType();

    /** @return a short description of the device-specific settings */
    protected abstract String getDeviceDetails();

    /** @return an energy-saving tip that takes the device's current settings into account */
    public abstract String getEnergySavingTip();

    /** @return a one-line summary of the device including its device-specific settings */
    public String getStatus() {
        return String.format(Locale.ROOT, "%-5s | %-10s | %-26s | %-3s | %6.1f W | %s",
                id, getType(), name, on ? "ON" : "OFF", powerRating, getDeviceDetails());
    }

    // ------------------------------------------------------------------ getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** @return the rated power in watts */
    public double getPowerRating() {
        return powerRating;
    }

    // ---------------------------------------------------------- object methods

    /** Devices are identified by their ID only. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SmartDevice)) {
            return false;
        }
        return id.equalsIgnoreCase(((SmartDevice) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return getStatus();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
