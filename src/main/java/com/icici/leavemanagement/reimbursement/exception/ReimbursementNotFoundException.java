package com.example.reimbursementservice.exception;

public class ReimbursementNotFoundException extends RuntimeException {

    public ReimbursementNotFoundException(String message) {
        super(message);
    }
}
