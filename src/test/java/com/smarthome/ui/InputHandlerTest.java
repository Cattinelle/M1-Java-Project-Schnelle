package com.smarthome.ui;

import com.smarthome.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class InputHandlerTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private InputHandler inputFrom(String... lines) {
        Scanner scanner = new Scanner(String.join("\n", lines) + "\n");
        return new InputHandler(scanner, new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    private String output() {
        return output.toString(StandardCharsets.UTF_8);
    }

    // Parsing and the custom exception

    @Test
    void parseIntAcceptsWholeNumbers() throws InvalidInputException {
        assertEquals(42, InputHandler.parseInt("42"));
        assertEquals(-3, InputHandler.parseInt(" -3 "));
    }

    @Test
    void parseIntRejectsTextAndDecimals() {
        InvalidInputException e = assertThrows(InvalidInputException.class, () -> InputHandler.parseInt("abc"));
        assertEquals("'abc' is not a whole number.", e.getMessage());
        assertThrows(InvalidInputException.class, () -> InputHandler.parseInt("2.5"));
    }

    @Test
    void parseIntRejectsEmptyInput() {
        InvalidInputException e = assertThrows(InvalidInputException.class, () -> InputHandler.parseInt("  "));
        assertEquals("This value must not be empty.", e.getMessage());
    }

    @Test
    void parseIntInRangeAcceptsBoundaries() throws InvalidInputException {
        assertEquals(1, InputHandler.parseIntInRange("1", 1, 5));
        assertEquals(5, InputHandler.parseIntInRange("5", 1, 5));
    }

    @Test
    void parseIntInRangeRejectsValuesOutsideRange() {
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> InputHandler.parseIntInRange("6", 1, 5));
        assertEquals("Please enter a number between 1 and 5.", e.getMessage());
        assertThrows(InvalidInputException.class, () -> InputHandler.parseIntInRange("0", 1, 5));
    }

    @Test
    void parseDoubleAcceptsDotAndComma() throws InvalidInputException {
        assertEquals(22.5, InputHandler.parseDouble("22.5"));
        assertEquals(22.5, InputHandler.parseDouble("22,5"));
        assertEquals(60.0, InputHandler.parseDouble("60"));
    }

    @Test
    void parseDoubleRejectsTextAndNonFiniteValues() {
        assertThrows(InvalidInputException.class, () -> InputHandler.parseDouble("warm"));
        assertThrows(InvalidInputException.class, () -> InputHandler.parseDouble("NaN"));
        assertThrows(InvalidInputException.class, () -> InputHandler.parseDouble("Infinity"));
    }

    @Test
    void parseYesNoAcceptsCommonAnswers() throws InvalidInputException {
        assertTrue(InputHandler.parseYesNo("y"));
        assertTrue(InputHandler.parseYesNo("YES"));
        assertFalse(InputHandler.parseYesNo("n"));
        assertFalse(InputHandler.parseYesNo("No"));
    }

    @Test
    void parseYesNoRejectsOtherAnswers() {
        assertThrows(InvalidInputException.class, () -> InputHandler.parseYesNo("maybe"));
    }

    // Reading with retries

    @Test
    void readIntInRangeAsksAgainUntilValid() {
        InputHandler input = inputFrom("abc", "9", "3");

        assertEquals(3, input.readIntInRange("Choice: ", 1, 5));
        assertTrue(output().contains("Error: 'abc' is not a whole number."));
        assertTrue(output().contains("Error: Please enter a number between 1 and 5."));
    }

    @Test
    void readRequiredTextRejectsEmptyLinesAndTrims() {
        InputHandler input = inputFrom("", "   ", "  Desk Lamp  ");

        assertEquals("Desk Lamp", input.readRequiredText("Name: "));
        assertTrue(output().contains("Error: This value must not be empty."));
    }

    @Test
    void readOptionalDoubleReturnsEmptyForEnter() {
        assertEquals(Optional.empty(), inputFrom("").readOptionalDouble("Hours: "));
    }

    @Test
    void readOptionalDoubleAsksAgainForInvalidNumber() {
        assertEquals(Optional.of(4.5), inputFrom("four", "4,5").readOptionalDouble("Hours: "));
    }

    @Test
    void readYesNoAsksAgainUntilValid() {
        assertTrue(inputFrom("ok", "y").readYesNo("Sure? "));
    }

    @Test
    void readingFromClosedInputThrowsNoSuchElementException() {
        InputHandler input = new InputHandler(new Scanner(""), new PrintStream(output));
        assertThrows(NoSuchElementException.class, () -> input.readLine("Anything: "));
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new InputHandler(null, System.out));
        assertThrows(IllegalArgumentException.class, () -> new InputHandler(new Scanner(""), null));
    }
}
