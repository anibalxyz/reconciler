package com.anibalxyz.features;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.validation.BodyValidator;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

public class Helper {
  @SuppressWarnings("unchecked")
  public static <T> OngoingStubbing<T> stubBodyValidatorFor(Context ctx, Class<T> clazz) {
    BodyValidator<T> mockValidator = (BodyValidator<T>) mock(BodyValidator.class);
    when(ctx.bodyValidator(clazz)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    return when(mockValidator.get());
  }

  public static <T> T capturedJsonAs(Context ctx, Class<T> clazz) {
    ArgumentCaptor<T> captor = ArgumentCaptor.forClass(clazz);
    verify(ctx).json(captor.capture());
    return captor.getValue();
  }

  public static Cookie capturedCookie(Context ctx) {
    ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
    verify(ctx).cookie(captor.capture());
    return captor.getValue();
  }
}
