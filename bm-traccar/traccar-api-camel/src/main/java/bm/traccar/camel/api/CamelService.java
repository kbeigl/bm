package bm.traccar.camel.api;

import bm.traccar.camel.api.Api.Devices;
import bm.traccar.generated.model.dto.Device;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * A simple Spring Service with an operation implemented with a Camel endpoint to get Devices from
 * traccar instance.
 */
@org.springframework.stereotype.Service
public class CamelService { // implements Api {

  @Value("${traccar.host:https://demo3.traccar.org}")
  private String host;

  @Value("${traccar.openapi:openapi-traccar-6.6.yaml}")
  private String specificationUri;

  @Autowired protected ProducerTemplate producer;
  @Autowired private CamelContext camelContext;

  Devices devices =
      new Api.Devices() {
        @Override
        public Device[] getById(int id) {
          Endpoint getDevicesEp = createOperationEndpoint("getDevices");
          final Map<String, Object> headers = new HashMap<String, Object>();
          headers.put("id", id);
          headers.put("Authorization", getAuthorization());
          return producer.requestBodyAndHeaders(getDevicesEp, null, headers, Device[].class);
        }
      };

  /**
   * Create Endpoint for openapi operation, i.e. operationId: getDevices
   *
   * @return configured Endpoint
   */
  protected Endpoint createOperationEndpoint(String operation) {
    String endpointUri = "rest-openapi:" + specificationUri + "#" + operation + "?host=" + host;
    Endpoint endpoint = camelContext.getEndpoint(endpointUri);
    if (endpoint == null) {
      throw new NoSuchEndpointException(endpointUri);
    }
    return endpoint;
  }

  @Value("${traccar.user.name}")
  private String usr;

  @Value("${traccar.user.password}")
  private String pwd;

  @Value("${traccar.apikey}")
  private String apikey;

  private String AUTHORIZATION = "init";

  /** AUTHORIZATION is set for the complete runtime and cannot be modified. */
  public String getAuthorization() {
    if (AUTHORIZATION.equals("init")) {
      setAuthorization();
    }
    return AUTHORIZATION;
  }

  /** AUTHORIZATION is fixed on first invocation. */
  private void setAuthorization() {
    System.out.println("log: initialize Authorization");
    if (usr != null && pwd != null)
      AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((usr + ":" + pwd).getBytes());
    else if (apikey != null) AUTHORIZATION = "Bearer " + apikey;
    else System.err.println("NO authorization for server " + host);
    // with usr/pwd/api
  }
}
