package com.mongo.kitchensink.util;

import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Utility class for testing exceptions.
 * Provides consistent exception testing across all test classes.
 */
public class ExceptionTestUtil {

    /**
     * Tests that the given executable throws the expected exception with the expected message.
     * 
     * @param expectedType the expected exception type
     * @param expectedMessage the expected exception message
     * @param executable the code that should throw the exception
     * @param <T> the exception type
     */
    public static <T extends Throwable> void assertThrowsWithMessage(
            Class<T> expectedType, 
            String expectedMessage, 
            Executable executable) {
        
        T exception = assertThrows(expectedType, executable);
        assertEquals(expectedMessage, exception.getMessage());
    }
} 