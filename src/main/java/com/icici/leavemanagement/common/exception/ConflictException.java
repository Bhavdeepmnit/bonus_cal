package com.icici.leavemanagement.common.exception;

/** Thrown when a request clashes with existing data (overlapping leave, task already decided...). Returns 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
