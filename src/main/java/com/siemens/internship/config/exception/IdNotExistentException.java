package com.siemens.internship.config.exception;

/**
 * Exception used when trying to find an item using a nonexistent id
 */

public class IdNotExistentException extends Exception {
    public IdNotExistentException(String message) {
        super(message);
    }
}
