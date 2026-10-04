package com.sunsetbeach.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hibernate.annotations.ValueGenerationType;

/**
 * Set once, on insert, to the current UTC time - never the JVM default zone. Use this, not
 * Hibernate's {@code @UtcCreationTimestamp}; see {@link UtcTimestampGenerator} for why.
 */
@ValueGenerationType(generatedBy = UtcTimestampGenerator.OnInsert.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface UtcCreationTimestamp {
}
