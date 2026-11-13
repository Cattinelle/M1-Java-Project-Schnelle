# M1-Java-Project-Schnelle

## Schnelle's Smart Home Energy Manager

A Java 17 console application for managing smart-home devices (lights, fans and thermostats) and estimating how much energy they use and what that electricity costs. The device data is loaded from a CSV file at start-up and saved after every change.

---

## Features

- **View devices**: a table of all devices (ID, name, type, ON/OFF, power rating), then the details of a selected device, including its setting and its current power draw.
- **Manage devices**
  - Add a SmartLight, SmartFan or Thermostat.
  - Remove a device, with a confirmation step.
  - Turn a device ON or OFF.
  - Change the device's setting: brightness 0–100 %, fan speed 1–5, or target temperature 10–30 °C.
- **Energy monitoring**
  - Enter the operating hours for each device. Press ENTER if a device did not run.
  - Shows each device's consumption and the total in kWh.
  - Estimates the electricity cost at a fixed tariff of €0.25/kWh. The tariff is shown before the cost.
  - The hours entered are reused for the cost estimate during the same session.
- **Persistence**: devices are loaded from `data/devices.csv` at start-up, saved after every add, remove, switch or setting change, and saved again on exit.
- **Help**: usage instructions inside the application.
- **Input validation**: invalid menu choices, non-numeric input, empty, invalid or duplicate IDs, unknown devices and out-of-range settings produce an error message, and the application keeps running.

---

## How to run

Requirements: JDK 17 or newer. Maven does not need to be installed, because the included Maven Wrapper downloads it on first use.

From the project folder (Windows PowerShell):

```powershell
.\mvnw.cmd -q compile exec:java
```

Or build a jar and run it:

```powershell
.\mvnw.cmd package
java -jar target\smarthome-energy-manager-1.0.0.jar
```

- On macOS/Linux, use `./mvnw` instead of `.\mvnw.cmd`.
- Start the application from the project folder, because `data/devices.csv` is a relative path.
- If `€` is shown as `?` in a Windows console, run `chcp 65001` first to switch the console to UTF-8.

## How to run the tests

```powershell
.\mvnw.cmd clean test
```

The JUnit 5 tests cover the device classes, the controller, the energy calculator, the CSV repository and the console menus, with normal, edge and invalid cases. File tests use temporary folders, so `data/devices.csv` is never changed. The menu tests run complete dialogues with scripted input.

---

## Project structure and main classes

```text
src/main/java/com/smarthome/
├── SmartHomeApp.java          entry point
├── interfaces/                Switchable, EnergyConsumable
├── model/                     SmartDevice (abstract), SmartLight, SmartFan, Thermostat
├── service/                   SmartHomeController, EnergyCalculator
├── repository/                DeviceRepository
├── ui/                        ConsoleMenu, DeviceManagementMenu, EnergyMenu, InputHandler
└── exception/                 InvalidInputException, DuplicateDeviceException
data/devices.csv               initial and saved device data
```

| Class | Responsibility |
|---|---|
| `SmartHomeApp` | Creates the objects, loads the devices and starts the menu. Stops if the data file cannot be read. |
| `SmartDevice` | Abstract base class with the shared state (ID, name, power rating, ON/OFF), validation, switching and the energy formula. |
| `SmartLight`, `SmartFan`, `Thermostat` | Each stores and validates its own setting and calculates its effective power from it. |
| `SmartHomeController` | Manages the registered devices: add (no duplicate IDs), remove, find, list, switch and change settings. |
| `EnergyCalculator` | Calculates device and total consumption from per-device hours, the cost, and the cost summary. It holds `TARIFF_PER_KWH`. |
| `DeviceRepository` | Loads and saves devices in the CSV file. |
| `ConsoleMenu` | Main menu, *View devices*, *Help* and *Exit*. |
| `DeviceManagementMenu` | Dialogues for adding, removing, switching and modifying devices, and saving after each change. |
| `EnergyMenu` | Collects the operating hours for the session and shows the consumption and cost. |
| `InputHandler` | Reads console input and asks again until the format is valid. |
| `InvalidInputException` | Custom exception for input that cannot be understood. |
| `DuplicateDeviceException` | Custom exception for registering a device with an ID that is already used. |

---

## OOP concepts

**Encapsulation.**
- The device classes keep their state in private fields, which can only be accessed or modified through their methods. For example, `setBrightness(150)` is rejected.
- `id` and `powerRating` are `final`.
- `SmartHomeController.getAllDevices()` returns a read-only copy of its internal list.

**Inheritance.**
- `SmartLight`, `SmartFan` and `Thermostat` extend `SmartDevice`.
- They inherit the validation, ON/OFF logic, `calculateConsumption()` and `getDeviceInfo()`.
- Each one extends `getDeviceInfo()` with `super.getDeviceInfo()` plus its own setting.

**Polymorphism.** Devices are stored and processed as `SmartDevice`. `EnergyCalculator.calculateTotalConsumption()` loops over `List<SmartDevice>` and calls `calculateConsumption(hours)` on each one. The result depends on each object's own `getEffectivePower()`:

| Type | Effective power |
|---|---|
| SmartLight | rating × brightness / 100 |
| SmartFan | rating × speed / 5 |
| Thermostat | rating × target temperature / 30 |

`getDeviceType()` and `getDeviceInfo()` are also overridden. `instanceof` is used where a type-specific setting has to be read or changed: the controller's setting methods, the setting dialogue and the CSV writer.

**Abstract class and interfaces.**
- `SmartDevice` is abstract because a generic "device" cannot calculate its own power. It declares `getEffectivePower()` and `getDeviceType()`, which each subclass must implement.
- Its `calculateConsumption()` is `final`, so every device follows the same rule: `kWh = effective power (W) × hours ÷ 1000`, 0 when OFF, invalid hours rejected.
- `Switchable` (`turnOn`, `turnOff`, `isOn`) and `EnergyConsumable` (`calculateConsumption`) define the two capabilities as small separate contracts. `SmartDevice` implements both once for all subclasses.

---

## Exception handling

- **Custom exception: `InvalidInputException`** (checked).
  - Thrown by the `InputHandler` parsers when typed text cannot be understood: `"abc"` for a number, `"maybe"` for yes/no, or an empty value.
  - The read methods catch it, display the message and ask again.
  - Because it is checked, the compiler forces this handling.
- **Custom exception: `DuplicateDeviceException`** (unchecked).
  - Thrown by `SmartHomeController.addDevice()` when a device with the same ID (ignoring case) is already registered. The message names the conflicting ID.
  - `DeviceManagementMenu` catches it, shows the message and does not add or save the device. `DeviceRepository` catches it while loading and skips the duplicate record, so the first record with an ID wins.
  - It is unchecked because callers can avoid it by checking `findDevice()` first, which the menu does to warn the user early.
- **`IllegalArgumentException`** for values that break a business rule, thrown by the model and the calculator.
  - Examples: an invalid ID, an empty name, a power rating ≤ 0, brightness, speed or temperature out of range, and negative or non-finite hours.
  - The menus catch it, display the model's message and either ask again or cancel the action.
- **`IOException`** for file problems: the file cannot be read or written, or it has a wrong CSV header. These are passed to the caller and reported to the user.
- **`NoSuchElementException`** when the console input is closed. `ConsoleMenu` catches it, saves the devices and exits normally.

---

## CSV data loading and persistence

One CSV file stores all device types. Columns that do not apply to a type are left empty:

```csv
id,name,type,powerRating,isOn,brightness,speed,targetTemperature
L1,Desk Lamp,LIGHT,60.0,true,75,,
F1,Ceiling Fan,FAN,50.0,false,,3,
T1,Living Room Heater,THERMOSTAT,1500.0,true,,,22.5
```

- **Start-up:** `SmartHomeApp` calls `DeviceRepository.loadInto(controller)`. For each record, the repository:
  - creates the correct subclass with its normal constructor and setter, so the model validation is reused,
  - restores ON/OFF with `turnOn()` or `turnOff()`,
  - registers the device with `controller.addDevice()`, which rejects duplicate IDs.
- **Missing or empty file:** a missing file is created with the header, and an empty file loads no devices.
- **Invalid records** are skipped, and the valid ones still load. Invalid means a wrong column count, an unknown type, a missing or out-of-range value, or an invalid ID.
- **Wrong header:** an `IOException` is reported, and the application stops instead of overwriting the file.
- **Saving:** `DeviceRepository.save(controller)` rewrites the whole file after every change and on exit. Operating hours are not saved; they only last for the current session.

**Limitations:**
- Skipped records are not reported to the user, and they disappear from the file at the next save.
- Commas in device names are saved as spaces.

---

## SOLID principles

These are the principles the code actually demonstrates:

- **Single Responsibility:** each class has one job.
  - Device rules live in the model.
  - `SmartHomeController` manages the collection.
  - `EnergyCalculator` does the arithmetic.
  - `DeviceRepository` handles the file.
  - The `ui` classes handle user interaction, and the console is split into three menus and an input reader instead of one large class.
- **Liskov Substitution:** every subclass can replace `SmartDevice` anywhere it is used. The `final` `calculateConsumption()` guarantees that all device types follow the same energy contract.
- **Open/Closed (energy calculation only):** A new device type must implement `getEffectivePower()` and `getDeviceType()`. `SmartDevice.calculateConsumption()` and `EnergyCalculator` work for it without changes. This does not apply to the whole application: the controller's setting methods, the setting dialogue and the CSV mapping contain type-specific code and would need a new case.
- **Interface Segregation:** switching and energy consumption are two small interfaces rather than one large one. In this project, only `SmartDevice` implements them.
- **Dependency Inversion (partly):** classes receive their collaborators through their constructors instead of creating them. For example, the menus receive the controller, repository, calculator, input and output. This is what lets the tests use a temporary file and scripted input. The dependencies are concrete classes, not interfaces.

---

## Technical decisions

1. **Validation lives in the model classes.** Constructors and setters enforce the rules (ID characters, ranges, a positive power rating). The UI only checks the input format, then shows the model's error message. Each rule exists once, so the UI and the model cannot disagree.
2. **A simple CSV format without quoting.** It is easy to read and explain. To keep the columns intact, device IDs may only contain letters, digits, `-` and `_`, and commas in names are saved as spaces.
3. **Duplicate IDs throw an exception, lookups return `boolean` or `Optional`.** Registering a duplicate ID breaks a business rule, so `addDevice()` throws `DuplicateDeviceException` and the caller cannot ignore it. A device that is not found is an expected situation in a menu, so `findDevice()` returns `Optional`, and remove, switch and the setting methods return `false`.
4. **Operating hours are stored in a `Map<SmartDevice, Double>` for the session.** Each device has its own hours, and a missing entry means the device did not run. Keeping the hours in memory means the cost estimate can reuse them without asking again.
5. **The application stops if the data file cannot be read.** Starting with an empty list and auto-saving would overwrite the user's file and lose their data.
