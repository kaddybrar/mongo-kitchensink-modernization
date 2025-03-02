package com.mongo.kitchensink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * MongoDB configuration class.
 * Configures MongoDB-specific settings.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    /**
     * Creates a validating MongoDB event listener.
     * This ensures that bean validation is applied to MongoDB documents.
     *
     * @param validator the validator factory bean
     * @return the validating MongoDB event listener
     */
    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(
            final LocalValidatorFactoryBean validator) {
        return new ValidatingMongoEventListener(validator);
    }
} 