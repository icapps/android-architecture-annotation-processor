package com.icapps.architecture.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this class is a viewmodel that should be added to the viewmodel factory.
 * This will be picked up by the arch annotation processor, so a binding will be added in the generated `GeneratedViewModelModule`.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateViewModelInjector {
}