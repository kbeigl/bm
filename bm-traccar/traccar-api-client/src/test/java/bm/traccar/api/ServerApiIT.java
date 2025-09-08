package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.generated.model.dto.Server;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApiIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(ServerApiIT.class);

  /** Test platonic API methods from interface for User DTO */
  @Test
  public void getServerInfo() {
    // works without authentication !?
    // api.setBearerToken(virtualAdmin);
    bm.traccar.generated.model.dto.Server server = api.getServerApi().getServerInfo();
    assertNotNull(server, "server info was not returned");
    logger.info("Server Info: {}", server);
  }

  @Test
  public void setServerRegistration() {
    // works without authentication !?
    // api.setBearerToken(virtualAdmin);
    Server server = api.getServerApi().getServerInfo();
    logger.info("Server Info: {}", server);

    if (!server.getRegistration()) {
      assertFalse(server.getRegistration());
      server.setRegistration(true);
      api.getServerApi().updateServer(server);
    }
    server = api.getServerApi().getServerInfo();
    logger.info("Server Registration: {}", server.getRegistration());
    assertTrue(server.getRegistration());
  }
}
