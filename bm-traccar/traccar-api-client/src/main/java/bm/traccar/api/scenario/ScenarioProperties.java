package bm.traccar.api.scenario;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

// @Component causes bean ambiguity with ScenarioConfig
@ConfigurationProperties(prefix = "scenario", ignoreUnknownFields = false)
@PropertySource("classpath:scenario.properties")
public class ScenarioProperties {

  private List<Device> device;

  public List<Device> getDevice() {
    return device;
  }

  public void setDevice(List<Device> device) {
    this.device = device;
  }

  // Nested static class for the 'user' properties
  // maybe implement ETL
  // from bm.traccar.rt.scenario.ScenarioProperties.User
  //   to bm.traccar.generated.model.dto.User
  public static class Device {
    String name, uniqueId, model;

    public void setName(String name) {
      this.name = name;
    }

    public void setUniqueId(String uniqueId) {
      this.uniqueId = uniqueId;
    }

    public void setModel(String model) {
      this.model = model;
    }
  }

  private List<User> user;

  public List<User> getUser() {
    return user;
  }

  public void setUser(List<User> user) {
    this.user = user;
  }

  public static class User {
    public String name, password, email;
    public boolean administrator;

    public void setAdministrator(boolean administrator) {
      this.administrator = administrator;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public void setEmail(String email) {
      this.email = email;
    }
    // access fields directly
    //    public String getName() { return name; }
    //    public String getPassword() { return password; }
    //    public String getEmail() { return email; }
    //    public boolean isAdministrator() { return administrator; }
  }
}
