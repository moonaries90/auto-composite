package com.huafon.common.auto.composite.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AutoComposite {

    FetchType fetchType() default FetchType.LAZY;

    String[] excludes() default "";
}
