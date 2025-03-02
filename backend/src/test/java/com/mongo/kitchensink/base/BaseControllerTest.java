package com.mongo.kitchensink.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongo.kitchensink.exception.GlobalExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Base class for controller tests.
 * Provides common setup for all controller tests.
 */
public abstract class BaseControllerTest extends BaseUnitTest {

    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sets up the MockMvc instance with the controller under test.
     * 
     * @param controller the controller to test
     */
    protected void setupMockMvc(Object controller) {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }
} 