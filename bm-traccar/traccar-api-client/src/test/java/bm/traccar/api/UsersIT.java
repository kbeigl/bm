package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bm.traccar.generated.model.dto.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Test <code>api/users</code> methods, @see <a
 * href="https://www.traccar.org/api-reference/#tag/Users">UsersApi</a> with ApiService. <br>
 * In database lingo create, update, delete User in traccar datamodel using generated entities.
 */
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = {ApiService.class})
@TestPropertySource("classpath:application.properties")
@Import(ApiConfig.class)
// without AOP !
@ExtendWith(ClientExceptionHandler.class)
public class UsersIT {

  @Value("${traccar.web.serviceAccountToken}")
  private String virtualAdmin;

  @Autowired private ApiService api;

  /** Test platonic API methods from interface for User DTO */
  @Test
  public void createUpdateDeleteUser() {

    //		api.setBasicAuth(mail, password);
    api.setBearerToken(virtualAdmin);

    // get nr of users, assert users++, back to nr
    int userNr = api.users.getUsers(null).size();

    // create / receive user
    User user = new User();
    // newUser.setId(5); // do not use!, generated by DB
    user.setName("user-1");
    user.setEmail("email-1"); // email syntax is not validated
    user.setPassword("pw-1"); // can't login UI without a password (useful!), backend can without

    User newUser_1 = api.users.createUser(user);
    assertEquals(userNr + 1, api.users.getUsers(null).size());

    // returns the generated id (asserts not null)
    int userId_1 = newUser_1.getId();

    // update user
    newUser_1.setEmail("email-1-b");

    User putUser = api.users.updateUser(userId_1, newUser_1);
    assertEquals(userNr + 1, api.users.getUsers(null).size());

    // delete user
    api.users.deleteUser(putUser.getId());
    assertEquals(userNr, api.users.getUsers(null).size());
  }
}
