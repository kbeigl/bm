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
