// src/main/java/com/mongo/kitchensink/exception/ErrorResponse.java
package com.mongo.kitchensink.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String message;
}