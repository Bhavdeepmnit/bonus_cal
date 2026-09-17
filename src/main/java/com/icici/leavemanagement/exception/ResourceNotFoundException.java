package com.icici.leavemanagement.exception;

/** Thrown when a record (employee, leave request, approval task...) does not exist. Returns 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id " + id);
    }
}
