package com.anibalxyz.features.users.application;

import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.InvalidEmailError;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for GetUserByEmail service")
public class GetUserByEmailTest extends UnitTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private GetUserByEmail getUserByEmail;

  @Test
  @DisplayName("execute: given a non-existing email, then return UserNotFoundError")
  public void execute_nonExistingEmail_returnUserNotFoundError() {
    String email = "non.existing@mail.com";
    when(userRepository.findByEmail(ResultAsserts.success(Email.of(email))))
        .thenReturn(Optional.empty());

    var result = getUserByEmail.execute(email);

    var failure = ResultAsserts.failure(result);
    assertThat(failure)
        .isInstanceOf(UserNotFoundError.class)
        .extracting(e -> ((UserNotFoundError) e).getReason())
        .isEqualTo(new UserNotFoundError.Reason.ByEmail(email));
  }

  @Test
  @DisplayName("execute: given a blank email, then return InvalidEmailError with Blank reason")
  public void execute_blankEmail_returnInvalidEmailErrorWithBlankReason() {
    var result = getUserByEmail.execute(" ");

    var failure = ResultAsserts.failure(result);
    assertThat(failure)
        .isInstanceOf(InvalidEmailError.class)
        .extracting(e -> ((InvalidEmailError) e).getReason())
        .isInstanceOf(InvalidEmailError.Reason.Blank.class);
  }

  @Test
  @DisplayName(
      "execute: given an invalid email format, then return InvalidEmailError with InvalidFormat reason")
  public void execute_invalidEmailFormat_returnInvalidEmailErrorWithInvalidFormatReason() {
    var result = getUserByEmail.execute("mailemail.com");

    var err = ResultAsserts.failure(result);
    assertThat(err)
        .isInstanceOf(InvalidEmailError.class)
        .extracting(e -> ((InvalidEmailError) e).getReason())
        .isEqualTo(new InvalidEmailError.Reason.InvalidFormat());
  }

  @Test
  @DisplayName("execute: given an existing email, then return the correct user")
  public void execute_existingEmail_returnUser() {
    User expected = VALID_USER;
    when(userRepository.findByEmail(expected.email())).thenReturn(Optional.of(expected));

    var result = getUserByEmail.execute(expected.email().value());

    User actual = ResultAsserts.success(result);
    assertThat(actual).isEqualTo(expected);
  }
}
