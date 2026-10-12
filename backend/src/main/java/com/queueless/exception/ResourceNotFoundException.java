package com.queueless.exception;

/**
 * Thrown when a requested resource is not found (e.g., user ID doesn't exist).
 *
 * The global exception handler converts this to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
