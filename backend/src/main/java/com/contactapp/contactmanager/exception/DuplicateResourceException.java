package com.contactapp.contactmanager.exception;

/** E.g. registering with an email or phone that is already taken -> 409 Conflict. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
