package com.anibalxyz.features.users.application;

import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.domain.error.ReasonedError;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.ResultAsserts;
import com.anibalxyz.shared.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for GetUserById service")
public class GetUserByIdTest extends UnitTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private GetUserById getUserById;

  @Test
  @DisplayName("getUserById: given an existing id, then return the correct user")
  public void getUserById_existingId_returnUser() {
    User expected = VALID_USER;
    when(userRepository.findById(expected.id())).thenReturn(Optional.of(expected));

    var result = getUserById.execute(expected.id());

    User actual = ResultAsserts.success(result);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @DisplayName("getUserById: given a non-existing id, then return UserNotFoundError")
  public void getUserById_nonExistingId_returnUserNotFoundError() {
    int id = 999;
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    var result = getUserById.execute(id);

    var failure = ResultAsserts.failure(result);
    assertThat(failure)
        .isInstanceOf(UserNotFoundError.class)
        .extracting(ReasonedError::getReason)
        .isEqualTo(new UserNotFoundError.Reason.ById(id));
  }
}
