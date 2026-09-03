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
    bm.traccar.generated.model.dto.Server server = api.server.getServerInfo();
    assertNotNull(server, "server info was not returned");
    logger.info("Server Info: {}", server);
  }

  @Test
  public void setServerRegistration() {
    // works without authentication !?
    // api.setBearerToken(virtualAdmin);
    Server server = api.server.getServerInfo();
    logger.info("Server Info: {}", server);

    if (!server.getRegistration()) {
      assertFalse(server.getRegistration());
      server.setRegistration(true);
      api.getServerApi().updateServer(server);
    }
    server = api.server.getServerInfo();
    logger.info("Server Registration: {}", server.getRegistration());
    assertTrue(server.getRegistration());
  }
}
