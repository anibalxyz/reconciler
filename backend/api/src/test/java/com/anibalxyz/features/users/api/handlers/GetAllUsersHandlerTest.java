package com.anibalxyz.features.users.api.handlers;

import static com.anibalxyz.shared.Constants.Users.buildUser;
import static com.anibalxyz.shared.Helpers.stubStatusChaining;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anibalxyz.features.common.api.out.response.success.CollectionResponse;
import com.anibalxyz.features.users.api.UserMapper;
import com.anibalxyz.features.users.application.GetAllUsers;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.shared.Constants;
import io.javalin.http.Context;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests for GetAllUsersHandler")
public class GetAllUsersHandlerTest {
  @Mock private GetAllUsers getAllUsers;
  @Mock private Context ctx;
  @InjectMocks private GetAllUsersHandler getAllUsersHandler;

  @BeforeAll
  public static void setup() {
    Constants.init();
  }

  @Test
  @DisplayName("getAllUsers: given there are users, then respond 200 with users list")
  public void getAllUsers_thereAreUsers_respond200WithUsersList() {
    List<User> fakeUsers = List.of(buildUser(1), buildUser(2));

    stubStatusChaining(ctx);
    when(getAllUsers.execute()).thenReturn(fakeUsers);
    getAllUsersHandler.handle(ctx);

    verify(ctx).status(200);
    var expected =
        CollectionResponse.ofSinglePage(
            fakeUsers.stream().map(UserMapper::toDetailResponse).toList());
    verify(ctx).json(expected);
  }

  @Test
  @DisplayName("getAllUsers: given there are no users, then respond 200 with empty list")
  public void getAllUsers_thereAreNoUsers_respond200WithEmptyList() {
    List<User> fakeUsers = List.of();

    stubStatusChaining(ctx);
    when(getAllUsers.execute()).thenReturn(fakeUsers);
    getAllUsersHandler.handle(ctx);

    verify(ctx).status(200);
    verify(ctx).json(CollectionResponse.ofSinglePage(List.of()));
  }
}
