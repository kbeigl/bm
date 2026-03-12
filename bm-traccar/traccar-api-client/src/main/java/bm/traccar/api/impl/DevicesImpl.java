package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.api.ApiHelper;
import bm.traccar.generated.api.DevicesApi;
import bm.traccar.generated.model.dto.Device;
import java.util.List;

public class DevicesImpl implements Api.Devices {
  private final DevicesApi devicesApi;

  public DevicesImpl(DevicesApi devicesApi) {
    this.devicesApi = devicesApi;
  }

  @Override
  public Device createDevice(Device device) {
    return devicesApi.devicesPost(device);
  }

  @Override
  public Device updateDevice(Long deviceId, Device device) {
    Integer integerId = ApiHelper.toInt(deviceId);
    return devicesApi.devicesIdPut(integerId, device);
  }

  @Override
  public void deleteDevice(Long deviceId) {
    Integer integerId = ApiHelper.toInt(deviceId);
    devicesApi.devicesIdDelete(integerId);
  }

  /** TODO implement individual parameters in dedicated methods */
  @Override
  public List<Device> getDevices(/* add params here*/ ) {
    // @Nullable Boolean all,		v6.12.2
    // @Nullable Integer userId,
    // @Nullable Integer id,
    // @Nullable String uniqueId,
    // @Nullable Boolean excludeAttributes
    return devicesApi.devicesGet(null, null, null, null, null);
  }
}
