// src/main/java/com/mongo/kitchensink/exception/DuplicateEmailException.java
package com.mongo.kitchensink.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}