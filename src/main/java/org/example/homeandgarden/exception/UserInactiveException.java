package org.example.homeandgarden.exception;

public class UserInactiveException extends RuntimeException {
    public UserInactiveException(String message) { super(message); }
}