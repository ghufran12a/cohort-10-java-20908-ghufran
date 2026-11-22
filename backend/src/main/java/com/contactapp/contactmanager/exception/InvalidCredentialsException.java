package com.contactapp.contactmanager.exception;

/** Wrong login details -> 401. The message is deliberately vague (see AuthService.login). */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email/phone or password");
    }
}
