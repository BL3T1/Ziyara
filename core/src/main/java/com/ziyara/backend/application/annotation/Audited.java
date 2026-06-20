package com.ziyara.backend.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit logging.
 * The AuditAspect in infrastructure.aop intercepts annotated methods and
 * writes an entry to the audit log after successful execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** Human-readable action name logged to sys_audit_logs (e.g. "USER_CREATE"). */
    String action();

    /** Entity type logged to the entity_type column (e.g. "User", "Booking"). */
    String entityType() default "";

    /**
     * Positional index (0-based) of the method argument that holds the entity ID.
     * -1 means "extract from the return value when it has an getId() method".
     */
    int entityIdArgIndex() default -1;
}
