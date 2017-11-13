package com.mkobit.gradle.test.kotlin.testkit.runner;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@TestTemplate
@ExtendWith(ToggleableCliArgumentTemplateContextProvider.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface BooleanFlags {
  BooleanFlag[] value();
}
