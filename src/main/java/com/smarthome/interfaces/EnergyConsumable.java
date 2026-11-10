package com.smarthome.interfaces;

/**
 * Capability of anything that consumes electrical energy.
 *
 * <p>Separate from {@link Switchable} because "consumes energy" and "can be toggled"
 * are independent capabilities (Interface Segregation Principle).</p>
 */
public interface EnergyConsumable {

    /**
     * Calculates the energy that would be consumed when operating for the given time
     * with the current settings.
     *
     * <p>Contract for all implementations:</p>
     * <ul>
     *   <li>the result is never negative;</li>
     *   <li>a switched-off device consumes {@code 0.0} kWh;</li>
     *   <li>negative or non-finite hours are rejected with an
     *       {@link com.smarthome.exception.InvalidEnergyValueException}.</li>
     * </ul>
     *
     * @param hours operating time in hours (must be &gt;= 0)
     * @return consumed energy in kilowatt-hours (kWh)
     */
    double calculateConsumption(double hours);
}
