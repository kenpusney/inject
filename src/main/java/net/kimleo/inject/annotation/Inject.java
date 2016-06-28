package net.kimleo.inject.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Inject {
    Class<?> value() default Object.class;
}
