package com.anibalxyz.core.primitives;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
public @interface ExcludeFromCoverageGenerated {
  String reason() default "";
}
