package com.contactapp.contactmanager.exception;

/** The client sent something that breaks a business rule (not a format rule) -> 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
