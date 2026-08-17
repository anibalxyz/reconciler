package com.anibalxyz.features.users.application;

import static com.anibalxyz.shared.Constants.Users.*;
import static com.anibalxyz.shared.NotificationAssert.assertThatNotification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.users.application.UpdateUserById.Error;
import com.anibalxyz.features.users.application.in.UpdateUserCommand;
import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.Name;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.*;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for UpdateUserById service")
public class UpdateUserByIdTest extends UnitTest {
  @Mock private UserRepository userRepository;

  private UpdateUserById updateUserById;

  @BeforeEach
  void deps() {
    UpdateUserById.Env env = Constants.APP_ENV;
    updateUserById = new UpdateUserById(env, userRepository);
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class Failure {

    @Test
    @DisplayName("updateUserById: given a non-existing id, then return NotFound error")
    public void updateUserById_nonExistingId_returnNotFoundError() {
      int id = 999;
      UpdateUserCommand command = new UpdateUserCommand("New Name", null, null);
      when(userRepository.findById(id)).thenReturn(Optional.empty());

      var result = updateUserById.execute(id, command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure)
          .isInstanceOf(UpdateUserById.Error.NotFound.class)
          .extracting(e -> ((UpdateUserById.Error.NotFound) e).error().getReason())
          .isEqualTo(new UserNotFoundError.Reason.ById(id));
    }

    @Test
    @DisplayName("updateUserById: given all fields empty, then return EmptyCommand error")
    public void updateUserById_emptyCommand_returnEmptyCommandFailure() {
      UpdateUserCommand command = new UpdateUserCommand(null, null, null);

      var result = updateUserById.execute(1, command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UpdateUserById.Error.EmptyCommand.class);
    }

    @Test
    @DisplayName("updateUserById: given a blank name, then return Blank error on name")
    public void updateUserById_blankName_returnBlankErrorOnName() {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand(" ", null, VALID_PASSWORD_STRING);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = updateUserById.execute(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UpdateUserById.Error.ValidationFailed.class);

      var notification = ((Error.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("name");
                assertThat(e.error())
                    .isInstanceOf(InvalidNameError.class)
                    .extracting(err -> ((InvalidNameError) err).getReason())
                    .isInstanceOf(InvalidNameError.Reason.Blank.class);
              });
    }

    @Test
    @DisplayName("updateUserById: given a blank email, then return Blank error on email")
    public void updateUserById_blankEmail_returnBlankErrorOnEmail() {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand(null, " ", VALID_PASSWORD_STRING);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = updateUserById.execute(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(Error.ValidationFailed.class);

      var notification = ((Error.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("email");
                assertThat(e.error())
                    .isInstanceOf(InvalidEmailError.class)
                    .extracting(err -> ((InvalidEmailError) err).getReason())
                    .isInstanceOf(InvalidEmailError.Reason.Blank.class);
              });
    }

    @Test
    @DisplayName("updateUserById: given a blank password, then return Blank error on password")
    public void updateUserById_blankPassword_returnBlankErrorOnPassword() {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand("New Name", null, " ");
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = updateUserById.execute(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(UpdateUserById.Error.ValidationFailed.class);

      var notification = ((Error.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("password");
                assertThat(e.error())
                    .isInstanceOf(InvalidPasswordError.class)
                    .extracting(err -> ((InvalidPasswordError) err).getReason())
                    .isInstanceOf(InvalidPasswordError.Reason.Blank.class);
              });
    }

    @ParameterizedTest
    @ValueSource(strings = {"some@mail", "some-mail.com", "@mail.com"})
    @DisplayName(
        "updateUserById: given an invalid email format, then return InvalidFormat error on email")
    public void updateUserById_invalidEmail_returnInvalidFormatErrorOnEmail(String email) {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand(null, email, null);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = updateUserById.execute(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(Error.ValidationFailed.class);

      var notification = ((Error.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("email");
                assertThat(e.error()).isInstanceOf(InvalidEmailError.class);
              });
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "short",
          "long-72-chars-12345678901234567890123456789012345678901234567890123456789"
        })
    @DisplayName(
        "updateUserById: given an invalid password value, then return InvalidValue error on password")
    public void updateUserById_invalidPassword_returnInvalidValueErrorOnPassword(String password) {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand(null, null, password);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));

      var result = updateUserById.execute(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(Error.ValidationFailed.class);

      var notification = ((Error.ValidationFailed) failure).notification();
      assertThat(notification.getErrors())
          .satisfiesExactly(
              e -> {
                assertThat(e.field()).isEqualTo("password");
                assertThat(e.error()).isInstanceOf(InvalidPasswordError.class);
              });
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use, then return EmailAlreadyTakenError on email")
    public void updateUserById_emailAlreadyInUse_returnEmailAlreadyTakenError() {
      User existing = VALID_USER;
      UpdateUserCommand command =
          new UpdateUserCommand(null, "taken" + existing.email().value(), null);
      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.findByEmail(ResultAsserts.success(Email.of(command.email()))))
          .thenReturn(Optional.of(existing));

      var result = updateUserById.execute(existing.id(), command);

      var failure = ResultAsserts.failure(result);
      assertThat(failure).isInstanceOf(Error.ValidationFailed.class);

      var notification = ((UpdateUserById.Error.ValidationFailed) failure).notification();
      assertThatNotification(notification).hasErrorOn("email", EmailAlreadyTakenError.class);
    }
  }

  @Nested
  @DisplayName("Success Scenarios")
  class Success {

    @Test
    @DisplayName("updateUserById: given a valid id and name, then return the updated user")
    public void updateUserById_validIdAndName_returnUpdatedUser() {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand("Valid Name", null, null);
      User expected = existing.withName(ResultAsserts.success(Name.of(command.name())));

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(expected)).thenAnswer(inv -> inv.getArgument(0));

      var result = updateUserById.execute(existing.id(), command);

      User actual = ResultAsserts.success(result);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and email, then return the updated user")
    public void updateUserById_validIdAndEmail_returnUpdatedUser() {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand(null, "new@mail.com", null);
      User expected = existing.withEmail(ResultAsserts.success(Email.of(command.email())));

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.findByEmail(expected.email())).thenReturn(Optional.empty());
      when(userRepository.save(expected)).thenAnswer(inv -> inv.getArgument(0));

      var result = updateUserById.execute(existing.id(), command);

      User actual = ResultAsserts.success(result);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("updateUserById: given a valid id and password, then return the updated user")
    public void updateUserById_validIdAndPassword_returnUpdatedUser() {
      User existing = VALID_USER;
      String newPassword = "new" + VALID_PASSWORD_STRING;
      UpdateUserCommand command = new UpdateUserCommand(null, null, newPassword);

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(
              argThat(
                  u ->
                      u.id().equals(existing.id())
                          && u.email().equals(existing.email())
                          && u.passwordMatches(newPassword))))
          .thenAnswer(inv -> inv.getArgument(0));

      var result = updateUserById.execute(existing.id(), command);

      User actual = ResultAsserts.success(result);
      assertThat(actual.passwordMatches(newPassword)).isTrue();
    }

    @Test
    @DisplayName(
        "updateUserById: given an email already in use by the user, then return the unmodified user")
    public void updateUserById_emailAlreadyUsedByUser_returnUnmodifiedUser() {
      User existing = VALID_USER;
      UpdateUserCommand command = new UpdateUserCommand(null, existing.email().value(), null);

      when(userRepository.findById(existing.id())).thenReturn(Optional.of(existing));
      when(userRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

      var result = updateUserById.execute(existing.id(), command);

      User user = ResultAsserts.success(result);
      assertThat(user).isEqualTo(existing);
    }
  }
}
