package bm.traccar.ws;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Singleton Spring bean to share the JSESSIONID between the REST login and websocket routes in a
 * thread-safe manner.
 */
@Component
public class SessionManager {

  private final AtomicReference<String> jsessionidCookie = new AtomicReference<>();

  public void setJsessionidCookie(String cookie) {
    this.jsessionidCookie.set(cookie);
  }

  public String getJsessionidCookie() {
    return this.jsessionidCookie.get();
  }
}
