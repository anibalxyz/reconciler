package com.anibalxyz.features.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.domain.error.ReasonedError;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.features.users.domain.error.UserNotFoundError;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.ResultAsserts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteUserByIdTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private DeleteUserById deleteUserById;

  @BeforeAll
  public static void setup() {
    Constants.init();
  }

  @Test
  @DisplayName("deleteUserById: given an existing id, then return success")
  public void deleteUserById_existingId_returnSuccess() {
    when(userRepository.deleteById(1)).thenReturn(true);

    var result = deleteUserById.execute(1);

    assertThat(ResultAsserts.success(result)).isNull();
  }

  @Test
  @DisplayName("deleteUserById: given a non-existing id, then return UserNotFoundError")
  public void deleteUserById_nonExistingId_returnUserNotFoundError() {
    int id = 999;
    when(userRepository.deleteById(id)).thenReturn(false);

    var result = deleteUserById.execute(id);

    var failure = ResultAsserts.failure(result);
    assertThat(failure)
        .isInstanceOf(UserNotFoundError.class)
        .extracting(ReasonedError::getReason)
        .isEqualTo(new UserNotFoundError.Reason.ById(id));
  }
}
