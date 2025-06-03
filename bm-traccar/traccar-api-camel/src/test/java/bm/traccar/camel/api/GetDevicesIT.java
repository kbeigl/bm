package bm.traccar.camel.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.traccar.generated.model.dto.Device;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@CamelSpringBootTest
@EnableAutoConfiguration
@TestPropertySource("classpath:application.properties")
@Import(Config.class)
public class GetDevicesIT {

  @Value("${traccar.host:https://demo3.traccar.org}")
  String host;

  // filename includes version String!
  @Value("${traccar.openapi:openapi-traccar-6.6.yaml}")
  String specificationUri;

  @Value("${traccar.user.name}")
  private String usr;

  @Value("${traccar.user.password}")
  private String pwd;

  @Value("${traccar.apikey}")
  private String apikey;

  @Value("${traccar.web.serviceAccountToken}")
  private String serviceAccount;

  @Autowired private ProducerTemplate producer;
  @Autowired private CamelContext camelContext;

  @Test
  public void shouldGetDeviceViaEndpoint() throws Exception {

    Endpoint getDevicesEp = createOperationEndpoint("getDevices");
    final Map<String, Object> headers = new HashMap<String, Object>();
    headers.put("id", "3");
    headers.put("Authorization", getAuthorization());

    final Device[] devices =
        producer.requestBodyAndHeaders(getDevicesEp, null, headers, Device[].class);
    System.out.println(devices.length);
    assertNotNull(devices, "request returned null");
  }

  private Endpoint createOperationEndpoint(String operation) {
    String endpointUri = "rest-openapi:" + specificationUri + "#" + operation + "?host=" + host;
    Endpoint endpoint = camelContext.getEndpoint(endpointUri);
    if (endpoint == null) {
      throw new NoSuchEndpointException(endpointUri);
    }
    return endpoint;
  }

  private String getAuthorization() {

    //	if (usr != null && pwd != null)
    //		return "Basic " + Base64.getEncoder().encodeToString((usr + ":" + pwd).getBytes());
    //	else if (apikey != null)
    //		return "Bearer " + apikey;
    //	else if (apikey != null)
    return "Bearer " + serviceAccount;
    //	else
    //		return ""; // not authorized
  }
}
