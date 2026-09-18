package com.icici.leavemanagement.common.exception;

/** Thrown when a request breaks a business rule (bad dates, no manager, no policy...). Returns 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
