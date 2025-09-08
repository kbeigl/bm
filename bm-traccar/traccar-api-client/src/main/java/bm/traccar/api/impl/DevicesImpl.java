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

  @Override
  public List<Device> getDevices(String userId) {
    return devicesApi.devicesGet(null, null, null, userId);
  }
}
