package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.generated.api.ServerApi;
import bm.traccar.generated.model.dto.Server;

public class ServerImpl implements Api.Server {
  private final ServerApi serverApi;

  public ServerImpl(ServerApi serverApi) {
    this.serverApi = serverApi;
  }

  @Override
  public Server getServerInfo() {
    return serverApi.serverGet();
  }

  @Override
  public Server updateServer(Server server) {
    return serverApi.serverPut(server);
  }
}
