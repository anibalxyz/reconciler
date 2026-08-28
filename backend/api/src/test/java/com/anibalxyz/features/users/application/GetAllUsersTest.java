package com.anibalxyz.features.users.application;

import static com.anibalxyz.shared.Constants.Users.buildUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.domain.UserRepository;
import com.anibalxyz.shared.UnitTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("Tests for GetAllUsers service")
public class GetAllUsersTest extends UnitTest {
  @Mock private UserRepository userRepository;

  @InjectMocks private GetAllUsers getAllUsers;

  @Test
  @DisplayName("getAllUsers: given users exist, then return a list of all users")
  public void getAllUsers_usersExist_returnListOfUsers() {
    List<User> expectedUsers = List.of(buildUser(1), buildUser(2));
    when(userRepository.findAll()).thenReturn(expectedUsers);

    assertThat(getAllUsers.execute()).isEqualTo(expectedUsers);
  }

  @Test
  @DisplayName("getAllUsers: given no users exist, then return an empty list")
  public void getAllUsers_noUsersExist_returnEmptyList() {
    when(userRepository.findAll()).thenReturn(List.of());

    assertThat(getAllUsers.execute()).isEmpty();
  }
}
