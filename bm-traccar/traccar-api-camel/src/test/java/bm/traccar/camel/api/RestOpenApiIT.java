package bm.traccar.camel.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.traccar.generated.model.dto.Device;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.rest.openapi.RestOpenApiComponent;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Simple OpenAPI tests with Endpoint-Configuration against local or remote server! <br>
 * Apply 'getDevices' operation w/o deviceId.
 */
@CamelSpringBootTest
@EnableAutoConfiguration
@TestPropertySource("classpath:application.properties")
@Import(Config.class)
public class RestOpenApiIT {

  @Value("${traccar.host:https://demo3.traccar.org}")
  private String host;

  @Value("${traccar.openapi:openapi-traccar-6.6.yaml}") // includes version String!
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

  @ParameterizedTest
  @MethodSource("openapicomponent")
  public void shouldGetDeviceViaEndpoint(String component) throws Exception {

    Endpoint getDevicesEp = resolveMandatoryEndpoint(configureEndpoint(component, "getDevices"));
    final Map<String, Object> headers = new HashMap<String, Object>();
    headers.put("id", "3");
    headers.put("Authorization", getAuthorization());

    final Device[] devices =
        producer.requestBodyAndHeaders(getDevicesEp, null, headers, Device[].class);
    System.out.println(devices.length);
    assertNotNull(devices, "request returned null");
  }

  private Endpoint resolveMandatoryEndpoint(String endpointUri) {
    Endpoint endpoint = camelContext.getEndpoint(endpointUri);
    if (endpoint == null) {
      throw new NoSuchEndpointException(endpointUri);
    }
    return endpoint;
  }

  /**
   * Test Jackson un/marshal().json()<br>
   * CamelJacksonEnableTypeConverter<br>
   * CamelJacksonTypeConverterToPojo<br>
   * and implicit testing of Pojos from maven generator<br>
   * return java object
   */
  @ParameterizedTest
  @MethodSource("openapicomponent")
  public void shouldGetDeviceByIdAsJavaObject(String component) throws Exception {

    String endpoint = configureEndpoint(component, "getDevices");
    System.out.println(endpoint);
    final Map<String, Object> headers = new HashMap<String, Object>();
    headers.put("id", "3");
    headers.put("Authorization", getAuthorization());

    final Device[] devices =
        producer.requestBodyAndHeaders(endpoint, null, headers, Device[].class);
    System.out.println(devices.length);
    assertNotNull(devices, "request returned null");
  }

  /**
   * Configure endpoint operation via String without parameters<br>
   * return json object in one String
   */
  @ParameterizedTest
  @MethodSource("openapicomponent")
  public void shouldGetDevicesAsJsonString(String component) throws Exception {

    String endpoint = configureEndpoint(component, "getDevices");
    System.out.println(endpoint);
    final Map<String, Object> headers = new HashMap<String, Object>();

    headers.put("Authorization", getAuthorization());
    final String body = producer.requestBodyAndHeaders(endpoint, null, headers, String.class);

    System.out.println(body);
    assertNotNull(body, "request returned null");
  }

  /**
   * Configure endpoint for openapicomponent and operation
   *
   * @return configured endpoint in String format
   */
  private String configureEndpoint(String openapicomponent, String operation) {
    return openapicomponent + specificationUri + "#" + operation + "?host=" + host;
  }

  /**
   * Evaluate authentications
   *
   * @return String for Authorization header
   */
  private String getAuthorization() {
    // if (usr != null && pwd != null)
    //	 return "Basic " + Base64.getEncoder().encodeToString((usr + ":" + pwd).getBytes());
    // else if (apikey != null)
    //	 return "Bearer " + apikey;
    // else if (apikey != null)
    return "Bearer " + serviceAccount;
    // else
    //	 return ""; // not authorized
  }

  /**
   * Run tests for different RestOpenApiComponents: <br>
   * "rest-openapi:" default Camel RestOpenApiComponent-name <br>
   * "traccar:" explicetly created via named @Bean RestOpenApiComponent
   */
  private static Iterable<String> openapicomponent() {
    return Arrays.asList(new String[] {"rest-openapi:", "traccar:"});
  }

  @Configuration
  static class TestConfig {

    @Bean
    Component traccar(CamelContext camelContext) {

      RestOpenApiComponent traccar = new RestOpenApiComponent(camelContext);
      // traccar.setSpecificationUri("traccar.json");		resources folder
      // traccar.setSpecificationUri(spec);
      // traccar.setHost(host); // and port
      // traccar.setBindingPackageScan(scanPackage);
      // traccar.setMissingOperation("ignore");

      // RestConfiguration restConfig = new RestConfiguration();
      // restConfig.setHost(host); // again ??
      // restConfig.setBindingMode("json");

      // camelContext.setRestConfiguration( restConfig );

      return traccar;
    }
  }
}
