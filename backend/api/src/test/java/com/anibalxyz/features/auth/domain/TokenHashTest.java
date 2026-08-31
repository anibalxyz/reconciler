package com.anibalxyz.features.auth.domain;

import static com.anibalxyz.shared.Constants.Auth.VALID_REFRESH_RAW_TOKEN_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anibalxyz.shared.UnitTest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenHashTest extends UnitTest {

  @Test
  @DisplayName("of: given rawToken, then generate correct SHA-256 hash")
  void of_validRawToken_generateSha256Hash() throws NoSuchAlgorithmException {
    RawToken rawToken = mock(RawToken.class);
    when(rawToken.value()).thenReturn(VALID_REFRESH_RAW_TOKEN_STRING);

    TokenHash tokenHash = TokenHash.of(rawToken);

    byte[] expectedHash =
        MessageDigest.getInstance("SHA-256")
            .digest(VALID_REFRESH_RAW_TOKEN_STRING.getBytes(StandardCharsets.UTF_8));

    assertThat(tokenHash.value()).isEqualTo(expectedHash);
  }

  @Test
  @DisplayName("reconstitute: given byte array, then return TokenHash with matching value")
  void reconstitute_validByteArray_returnTokenHash() {
    byte[] inputBytes = new byte[] {1, 2, 3, 4, 5};

    TokenHash tokenHash = TokenHash.reconstitute(inputBytes);

    assertThat(tokenHash.value()).isEqualTo(inputBytes);
  }

  @Test
  @DisplayName("value: when invoked, then return a defensive copy")
  void value_whenInvoked_returnDefensiveCopy() {
    byte[] originalBytes = new byte[] {10, 20, 30};
    TokenHash tokenHash = TokenHash.reconstitute(originalBytes);

    byte[] extractedBytes = tokenHash.value();
    extractedBytes[0] = 99;

    assertThat(tokenHash.value()).isEqualTo(new byte[] {10, 20, 30});
  }
}
