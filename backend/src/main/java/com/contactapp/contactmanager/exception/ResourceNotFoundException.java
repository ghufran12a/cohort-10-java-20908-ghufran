package com.contactapp.contactmanager.exception;

/**
 * Thrown when something does not exist (or does not belong to the user).
 * In the exception-handling phase, @ControllerAdvice will turn this into a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
