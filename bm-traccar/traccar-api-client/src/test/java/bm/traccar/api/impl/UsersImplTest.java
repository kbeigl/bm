/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bm.traccar.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bm.traccar.api.ApiErrorCode;
import bm.traccar.api.ApiException;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.generated.model.dto.User;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsersImplTest {

  private UsersApi usersApi;
  private UsersImpl users;

  @BeforeEach
  void setUp() {
    usersApi = mock(UsersApi.class);
    users = new UsersImpl(usersApi);
  }

  @Test
  void getUser_whenUserFound_returnsUser() {
    User user = new User();
    user.setId(42L);
    user.setName("alice");

    when(usersApi.usersGet(null, null, null, null)).thenReturn(List.of(user));

    User result = users.getUser(42L);
    assertNotNull(result);
    assertEquals(42L, result.getId());
    assertEquals("alice", result.getName());
  }

  @Test
  void getUser_whenUserNotFound_throwsApiException() {
    User user = new User();
    user.setId(42L);

    when(usersApi.usersGet(null, null, null, null)).thenReturn(List.of(user));

    ApiException ex = assertThrows(ApiException.class, () -> users.getUser(99L));
    assertEquals("User with id 99 not found", ex.getMessage());
    assertEquals(ApiErrorCode.NOT_FOUND, ex.getErrorCode());
    assertEquals(404, ex.getStatusCode());
  }

  @Test
  void getUser_whenUsersListEmpty_throwsApiException() {
    when(usersApi.usersGet(null, null, null, null)).thenReturn(Collections.emptyList());

    ApiException ex = assertThrows(ApiException.class, () -> users.getUser(1L));
    assertEquals("User with id 1 not found", ex.getMessage());
    assertEquals(ApiErrorCode.NOT_FOUND, ex.getErrorCode());
    assertEquals(404, ex.getStatusCode());
  }

  @Test
  void getUser_whenUsersListNull_throwsApiException() {
    when(usersApi.usersGet(null, null, null, null)).thenReturn(null);

    ApiException ex = assertThrows(ApiException.class, () -> users.getUser(1L));
    assertEquals("User with id 1 not found", ex.getMessage());
    assertEquals(ApiErrorCode.NOT_FOUND, ex.getErrorCode());
    assertEquals(404, ex.getStatusCode());
  }

  @Test
  void updateUser_whenIdOutOfRange_throwsApiException() {
    User user = new User();
    Long outOfRangeId = (long) Integer.MAX_VALUE + 1L;

    ApiException ex = assertThrows(ApiException.class, () -> users.updateUser(outOfRangeId, user));
    assertNotNull(ex.getCause());
    assertEquals(ArithmeticException.class, ex.getCause().getClass());
    assertEquals(ApiErrorCode.BAD_REQUEST, ex.getErrorCode());
    assertEquals(400, ex.getStatusCode());
  }

  @Test
  void deleteUser_whenIdOutOfRange_throwsApiException() {
    Long outOfRangeId = (long) Integer.MAX_VALUE + 1L;

    ApiException ex = assertThrows(ApiException.class, () -> users.deleteUser(outOfRangeId));
    assertNotNull(ex.getCause());
    assertEquals(ArithmeticException.class, ex.getCause().getClass());
    assertEquals(ApiErrorCode.BAD_REQUEST, ex.getErrorCode());
    assertEquals(400, ex.getStatusCode());
  }

  @Test
  void createUser_delegatesToUsersApi() {
    User user = new User();
    when(usersApi.usersPost(user)).thenReturn(user);

    User result = users.createUser(user);
    assertEquals(user, result);
    verify(usersApi).usersPost(user);
  }

  @Test
  void createUserWithCredentials_createsUserWithAdminFalse() {
    when(usersApi.usersPost(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, User.class));

    User result = users.createUserWithCredentials("alice", "secret", "alice@example.com");
    assertNotNull(result);
    assertEquals("alice", result.getName());
    assertEquals("secret", result.getPassword());
    assertEquals("alice@example.com", result.getEmail());
    assertFalse(result.getAdministrator());
  }

  @Test
  void createAdminWithCredentials_createsUserWithAdminTrue() {
    when(usersApi.usersPost(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, User.class));

    User result = users.createAdminWithCredentials("bob", "secret", "bob@example.com");
    assertNotNull(result);
    assertEquals("bob", result.getName());
    assertEquals("secret", result.getPassword());
    assertEquals("bob@example.com", result.getEmail());
    assertTrue(result.getAdministrator());
  }

  @Test
  void createManagerWithCredentials_withUserLimit_createsManager() {
    when(usersApi.usersPost(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, User.class));

    User result = users.createManagerWithCredentials("mgr", "secret", "mgr@example.com", 5);
    assertNotNull(result);
    assertEquals("mgr", result.getName());
    assertEquals("secret", result.getPassword());
    assertEquals("mgr@example.com", result.getEmail());
    assertFalse(result.getAdministrator());
    assertEquals(5, result.getUserLimit());
  }

  @Test
  void createManagerWithCredentials_withUserAndDeviceLimit_createsManager() {
    when(usersApi.usersPost(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, User.class));

    User result = users.createManagerWithCredentials("mgr", "secret", "mgr@example.com", 5, 10);
    assertNotNull(result);
    assertEquals("mgr", result.getName());
    assertEquals("secret", result.getPassword());
    assertEquals("mgr@example.com", result.getEmail());
    assertFalse(result.getAdministrator());
    assertEquals(5, result.getUserLimit());
    assertEquals(10, result.getDeviceLimit());
  }

  @Test
  void createManagerWithCredentials_withZeroUserLimit_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> users.createManagerWithCredentials("mgr", "secret", "mgr@example.com", 0));
    assertThrows(
        IllegalArgumentException.class,
        () -> users.createManagerWithCredentials("mgr", "secret", "mgr@example.com", 0, 10));
  }
}
