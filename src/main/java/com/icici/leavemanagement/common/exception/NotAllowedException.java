package com.icici.leavemanagement.common.exception;

/** Thrown when the logged-in user may not perform the action (e.g. wrong approver). Returns 403. */
public class NotAllowedException extends RuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }
}
