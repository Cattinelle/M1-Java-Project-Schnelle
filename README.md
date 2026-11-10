# M1-Java-Project-Schnelle

## Schnelle's Smart Home Energy Manager

A Java 17 console application for managing smart-home devices and estimating their energy consumption and electricity cost. It was built as an object-oriented programming final project, in five phases: device model, device management, CSV persistence, energy monitoring and the console application.

---

## Features

- **View devices**: a table of every registered device (ID, name, type, ON/OFF status, power rating), then details for a selected device, including its specific setting and its current power draw.
- **Manage devices**
  - Add a SmartLight, SmartFan or Thermostat. Duplicate IDs are rejected and all inputs are validated.
  - Remove a device, with a confirmation step.
  - Turn a device ON or OFF.
  - Modify the device-specific setting: brightness 0–100 %, fan speed 1–5, or target temperature 10–30 °C.
- **Energy monitoring**
  - Enter the operating hours for each device. Press ENTER if a device did not run.
  - Shows each device's consumption and the household total in kWh.
  - Estimates the electricity cost with a fixed tariff of **€0.25/kWh**. The tariff is shown before the cost, and the hours entered earlier in the session are reused.
- **Persistence**: devices are loaded from `data/devices.csv` at start-up, saved after every change, and saved again on exit.
- **Help**: a short guide inside the application.
- **Robust input**: invalid menu choices, text where a number is expected, empty IDs, unknown devices and out-of-range values all produce a clear message, and the application keeps running.

---

## Requirements

- JDK 17 or newer (`java -version`).
- Nothing else. Maven is downloaded automatically by the included **Maven Wrapper**.

## Running the application

From the project folder (Windows PowerShell):

```powershell
.\mvnw.cmd -q compile exec:java
```

Or build a runnable jar first:

```powershell
.\mvnw.cmd package
java -jar target\smarthome-energy-manager-1.0.0.jar
```

On macOS/Linux, use `./mvnw` instead of `.\mvnw.cmd`.

Start the application from the project folder, because the data file path `data/devices.csv` is relative to the current folder.

**€ sign shows as `?`:** classic Windows consoles use a code page that has no euro sign. Switch the console to UTF-8 before starting the application:

```powershell
chcp 65001
```

## Running the tests

```powershell
.\mvnw.cmd clean test
```

147 JUnit 5 tests cover the model, the services, the repository and the console classes. The tests use temporary folders, so `data/devices.csv` is never changed by a test run.

---

## Project structure

```text
src/main/java/com/smarthome/
├── SmartHomeApp.java                 entry point
├── interfaces/
│   ├── Switchable.java               turnOn(), turnOff(), isOn()
│   └── EnergyConsumable.java         calculateConsumption(hours)
├── model/
│   ├── SmartDevice.java              abstract base class
│   ├── SmartLight.java               brightness 0–100 %
│   ├── SmartFan.java                 speed 1–5
│   └── Thermostat.java               target temperature 10–30 °C
├── service/
│   ├── SmartHomeController.java      device management
│   └── EnergyCalculator.java         consumption totals and cost
├── repository/
│   └── DeviceRepository.java         CSV loading and saving
├── ui/
│   ├── ConsoleMenu.java              main menu, view, help, exit
│   ├── DeviceManagementMenu.java     add / remove / switch / modify
│   ├── EnergyMenu.java               operating hours, kWh and cost
│   └── InputHandler.java             reading and parsing console input
└── exception/
    └── InvalidInputException.java    custom checked exception
data/devices.csv                      saved devices
```

## Main classes and responsibilities

| Class | Responsibility |
|---|---|
| `SmartHomeApp` | Creates the objects, loads the devices and starts the menu. Stops if the data file cannot be read, so it is not overwritten. |
| `SmartDevice` | Holds the shared state (ID, name, power rating, ON/OFF), the switching logic, validation, and the energy formula. |
| `SmartLight`, `SmartFan`, `Thermostat` | Store and validate their own setting, and calculate their effective power from it. |
| `SmartHomeController` | Keeps the registered devices in a private `List<SmartDevice>`: add (no duplicate IDs), remove, find (`Optional`), list, switch and update settings. |
| `EnergyCalculator` | Calculates device and total consumption for individual operating hours, the cost, and the cost summary text. It holds the tariff constant `TARIFF_PER_KWH`. |
| `DeviceRepository` | Reads and writes `data/devices.csv`. |
| `ConsoleMenu` | Shows the main menu and passes each choice on. Handles *View*, *Help* and *Exit*. |
| `DeviceManagementMenu` | User dialogue for adding, removing, switching and modifying devices, plus auto-saving. |
| `EnergyMenu` | Collects the operating hours (kept in memory for the session) and displays the consumption and cost. |
| `InputHandler` | Reads a line and asks again until its format is valid (number, y/n, non-empty text). |
| `InvalidInputException` | Signals input text that cannot be understood. |

---

## OOP concepts

### Encapsulation
All fields are `private`. Values can only change through methods that validate them, for example `setBrightness()` rejects 150. `id` and `powerRating` are `final`. `SmartHomeController.getAllDevices()` returns a read-only copy, so the internal list cannot be changed from outside.

### Inheritance
`SmartLight`, `SmartFan` and `Thermostat` extend `SmartDevice`. They inherit the constructor validation, the ON/OFF logic, `calculateConsumption()` and `getDeviceInfo()`. Each subclass extends `getDeviceInfo()` with `super.getDeviceInfo()` plus its own setting.

### Polymorphism
The controller, the calculator, the repository and the menus all work with `SmartDevice` references. A call such as `device.calculateConsumption(hours)` uses the correct `getEffectivePower()` for each type:

| Type | Effective power |
|---|---|
| SmartLight | rating × brightness / 100 |
| SmartFan | rating × speed / 5 |
| Thermostat | rating × target temperature / 30 |

`getDeviceType()` and `getDeviceInfo()` are also overridden. Type checks with `instanceof` are used only where a type-specific setter is needed: changing a setting, and writing the CSV columns.

### Abstract classes and interfaces
- `Switchable` and `EnergyConsumable` are interfaces describing two separate capabilities: being switched, and consuming energy.
- `SmartDevice` is an **abstract class** that implements both interfaces once, so the subclasses share that code.
- It declares two abstract methods that each subclass must provide: `getDeviceType()` and `getEffectivePower()`.
- `calculateConsumption()` is `final`, so the formula `Energy (kWh) = Effective Power (W) × Hours ÷ 1000`, the zero-when-OFF rule and the hours validation cannot be changed by a subclass.

### Exceptions
- **Custom exception `InvalidInputException`** (checked). The `InputHandler` parsers throw it when typed text cannot be understood, for example `"abc"` for a number, `"maybe"` for y/n, or an empty value. The read methods catch it, show its message and ask again. Making it checked means the compiler forces this handling.
- **`IllegalArgumentException`** for invalid values: model rules such as brightness 0–100 or a positive power rating, and invalid operating hours or energy values. The menus catch it and show the model's own message, so validation rules are not duplicated in the UI.
- **`IOException`** for real file problems: the file can't be read or written, or it has a wrong header. These are reported to the user and never silently ignored.
- **`NoSuchElementException`** from the `Scanner` when the input stream is closed (Ctrl+Z / Ctrl+D). The application then saves and exits cleanly.

---

## CSV data loading and persistence

One file stores all device types. Columns that do not apply to a type stay empty:

```csv
id,name,type,powerRating,isOn,brightness,speed,targetTemperature
L1,Desk Lamp,LIGHT,60.0,true,75,,
F1,Ceiling Fan,FAN,50.0,false,,3,
T1,Living Room Heater,THERMOSTAT,1500.0,true,,,22.5
```

- **Start-up**
  - `DeviceRepository.loadInto(controller)` rebuilds each device with its normal constructor and setter, restores ON/OFF with `turnOn()`/`turnOff()`, and registers it through `controller.addDevice()`, which also rejects duplicate IDs.
  - If the file is missing, it is created with the header.
  - An empty file loads nothing.
  - Invalid records are skipped: wrong column count, unknown type, missing or out-of-range values. The valid records still load.
  - A wrong header is reported as an error, and the application stops instead of overwriting the file.
- **Saving**
  - Saving happens after every add, remove, switch and modify, and again on exit as a safeguard.
  - Commas in device names are saved as spaces, because a comma would split the CSV columns. The user is warned when entering such a name.
- **Not saved:** operating hours are kept in memory for the current session only.

---

## SOLID principles

- **Single Responsibility:** each class has one job. The models hold device rules, the controller manages the collection, the calculator does the energy arithmetic, the repository handles files, and the UI classes talk to the user. `InputHandler` only parses input, and the three menu classes each handle one menu.
- **Open/Closed:** a new device type can be added by writing a new `SmartDevice` subclass. The controller and the calculator work unchanged, because they rely on `getEffectivePower()` through polymorphism. Only the UI and the CSV mapping need a new case.
- **Liskov Substitution:** every subclass can be used wherever a `SmartDevice` is expected. The `final` `calculateConsumption()` guarantees that all of them follow the same energy contract.
- **Interface Segregation:** switching (`Switchable`) and energy use (`EnergyConsumable`) are separate small interfaces instead of one large one.
- **Dependency Inversion (applied simply):** dependencies are passed in through constructors. `SmartHomeApp` creates the objects, and the menus receive the controller, repository, calculator, input and output. Tests can therefore pass a temporary CSV file and scripted input. No extra interfaces were created only for this, to keep the project small.

---

## Technical decisions

- **Validation in one place.** Range rules live in the model setters and constructors. The UI only checks the input format, then calls the model and shows its error message. This avoids duplicated, possibly conflicting rules.
- **`boolean` results and `Optional` in the controller.** "Device not found" and "duplicate ID" are normal situations in a menu, so the controller reports them with `false` or `Optional.empty()` instead of exceptions.
- **Case-insensitive IDs.** `l1` finds `L1`, and `L1` and `l1` cannot both exist. This is friendlier for console input.
- **Operating hours in a `Map<SmartDevice, Double>`.** Each device has its own hours. A missing entry means the device did not run. Hours are validated immediately when entered, by reusing `EnergyCalculator.calculateDeviceConsumption()`.
- **The tariff is defined once.** `EnergyCalculator.TARIFF_PER_KWH`. The displayed tariff and the cost calculation both use this constant.
- **`Locale.ROOT` for numbers in output and CSV.** Decimals always use a dot (`22.5`), whatever the system's language settings. Input accepts both `22.5` and `22,5`.
- **Simple CSV without quoting.** Easier to read and explain. The trade-off is that commas are not allowed in names.
- **Stop when the file cannot be loaded.** Starting with an empty list and auto-saving would overwrite and lose the user's data.
- **Testable console.** The menus write to a `PrintStream` and read through a `Scanner` passed in from outside, so complete menu dialogues are tested with scripted input.
- **No external libraries.** Only the Java standard library, plus JUnit 5 for tests.
