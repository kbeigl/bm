package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.generated.api.PermissionsApi;
import bm.traccar.generated.model.dto.Permission;

public class PermissionsImpl implements Api.Permissions {
  private final PermissionsApi permissionsApi;

  public PermissionsImpl(PermissionsApi permissionsApi) {
    this.permissionsApi = permissionsApi;
  }

  @Override
  public void createPermission(Permission permission) {
    permissionsApi.permissionsPost(permission);
  }

  @Override
  public void deletePermission(Permission permission) {
    permissionsApi.permissionsDelete(permission);
  }
}
