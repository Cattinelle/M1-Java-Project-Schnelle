package com.smarthome.ui;

import com.smarthome.exception.InvalidInputException;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

//InputHandler:
//Reads console input and keeps asking until the input has the right format.
//It only checks the format; device rules (ranges etc.) stay in the model classes.
public class InputHandler {

    private final Scanner scanner;
    private final PrintStream out;

    public InputHandler(Scanner scanner, PrintStream out) {
        if (scanner == null || out == null) {
            throw new IllegalArgumentException("Scanner and output must not be null.");
        }
        this.scanner = scanner;
        this.out = out;
    }

    // Reading input

    public String readLine(String prompt) {
        out.print(prompt);
        return scanner.nextLine().trim(); // throws NoSuchElementException when input is closed
    }

    public String readRequiredText(String prompt) {
        return readUntilValid(prompt, InputHandler::requireText);
    }

    public int readInt(String prompt) {
        return readUntilValid(prompt, InputHandler::parseInt);
    }

    public int readIntInRange(String prompt, int min, int max) {
        return readUntilValid(prompt, text -> parseIntInRange(text, min, max));
    }

    public double readDouble(String prompt) {
        return readUntilValid(prompt, InputHandler::parseDouble);
    }

    public Optional<Double> readOptionalDouble(String prompt) {
        return readUntilValid(prompt, text -> text.isEmpty() ? Optional.empty() : Optional.of(parseDouble(text)));
    }

    public boolean readYesNo(String prompt) {
        return readUntilValid(prompt, InputHandler::parseYesNo);
    }

    private <T> T readUntilValid(String prompt, Parser<T> parser) {
        while (true) {
            try {
                return parser.parse(readLine(prompt));
            } catch (InvalidInputException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    private interface Parser<T> {
        T parse(String text) throws InvalidInputException;
    }

    // Parsing (package-private so they can be tested directly)

    static String requireText(String text) throws InvalidInputException {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException("This value must not be empty.");
        }
        return text.trim();
    }

    static int parseInt(String text) throws InvalidInputException {
        try {
            return Integer.parseInt(requireText(text));
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + text.trim() + "' is not a whole number.");
        }
    }

    static int parseIntInRange(String text, int min, int max) throws InvalidInputException {
        int value = parseInt(text);
        if (value < min || value > max) {
            throw new InvalidInputException("Please enter a number between " + min + " and " + max + ".");
        }
        return value;
    }

    static double parseDouble(String text) throws InvalidInputException {
        String value = requireText(text).replace(',', '.'); // accepts 22,5 as well as 22.5
        try {
            double number = Double.parseDouble(value);
            if (!Double.isFinite(number)) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + text.trim() + "' is not a valid number.");
        }
    }

    static boolean parseYesNo(String text) throws InvalidInputException {
        String answer = requireText(text).toLowerCase(Locale.ROOT);
        if (answer.equals("y") || answer.equals("yes")) {
            return true;
        }
        if (answer.equals("n") || answer.equals("no")) {
            return false;
        }
        throw new InvalidInputException("Please answer y (yes) or n (no).");
    }
}
