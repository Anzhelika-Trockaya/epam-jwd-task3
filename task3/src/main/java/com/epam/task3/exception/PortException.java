package com.epam.task3.exception;

public class PortException extends Exception {
    private static final long serialVersionUID = 7241895370059306641L;

    public PortException() {
        super();
    }

    public PortException(String message) {
        super(message);
    }

    public PortException(String message, Throwable cause) {
        super(message, cause);
    }

    public PortException(Throwable cause) {
        super(cause);
    }
}
