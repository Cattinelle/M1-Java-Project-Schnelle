package com.smarthome.exception;

//DuplicateDeviceException:
//Thrown when a device is registered with an ID that is already in use.
//Unchecked, because callers can avoid it by checking findDevice() first.
public class DuplicateDeviceException extends RuntimeException {

    public DuplicateDeviceException(String message) {
        super(message);
    }
}
