package com.myshop.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as idempotent: the client must send an
 * Idempotency-Key header, and retries with the same key replay the original
 * response instead of re-executing the operation.
 *
 * Enforcement lives in IdempotencyAspect — annotating a method is all that is
 * needed to protect a new endpoint (Open/Closed: no service code changes).
 * The annotated method must return ResponseEntity (all controllers here do).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
}
