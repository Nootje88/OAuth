package com.template.OAuth.annotation;

import com.template.OAuth.enums.AuditEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Type of audit event
     */
    AuditEventType type();

    /**
     * Description of the audit event
     */
    String description() default "";

    /**
     * Whether to include method arguments in the audit log
     */
    boolean includeArgs() default false;

    /**
     * Whether to include method result in the audit log
     */
    boolean includeResult() default false;
}