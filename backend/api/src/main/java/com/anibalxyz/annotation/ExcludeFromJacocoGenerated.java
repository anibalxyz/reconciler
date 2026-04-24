package com.anibalxyz.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ExcludeFromJacocoGenerated {
  String reason() default "";
}
