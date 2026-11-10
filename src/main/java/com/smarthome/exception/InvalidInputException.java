package com.smarthome.exception;

//InvalidInputException:
//Thrown when text typed by the user cannot be understood (e.g. "abc" for a number).
//Checked exception, so callers must decide how to handle it.
public class InvalidInputException extends Exception {

    public InvalidInputException(String message) {
        super(message);
    }
}
