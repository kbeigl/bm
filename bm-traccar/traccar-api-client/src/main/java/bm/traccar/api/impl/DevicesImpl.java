package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.api.ApiHelper;
import bm.traccar.generated.api.DevicesApi;
import bm.traccar.generated.model.dto.Device;
import java.util.List;
import org.springframework.web.client.RestClientResponseException;

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

  /**
   * TODO implement individual parameters in dedicated methods and replace own filtering methods
   * created earlier
   *
   * <p>Fetch a list of Devices Without any params, returns a list of the user&#39;s devices
   *
   * <p><b>200</b> - OK
   *
   * <p><b>400</b> - No permission
   *
   * @param all Can only be used by admins or managers to fetch all entities
   * @param userId Standard users can use this only with their own _userId_
   * @param id To fetch one or more devices. Multiple params can be passed like
   *     &#x60;id&#x3D;31&amp;id&#x3D;42&#x60;
   * @param uniqueId To fetch one or more devices. Multiple params can be passed like
   *     &#x60;uniqueId&#x3D;333331&amp;uniqieId&#x3D;44442&#x60;
   * @param excludeAttributes Exclude attributes field from device payload
   * @param limit Limit the number of returned results
   * @param offset Offset for pagination
   * @param keyword Search keyword filter (searches name, uniqueId, phone, model, contact)
   * @return List&lt;Device&gt;
   * @throws RestClientResponseException if an error occurs while attempting to invoke the API
   */
  @Override
  public List<Device> getDevices(/* add params here*/ ) {
    // all fields are @jakarta.annotation.Nullable	 v6.14.2
    // Boolean all,
    // Integer userId,
    // Integer id,
    // String uniqueId,
    // Boolean excludeAttributes,
    // Integer limit,
    // Integer offset,
    // String keyword
    return devicesApi.devicesGet(null, null, null, null, null, null, null, null);
  }
}
