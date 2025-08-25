package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.traccar.generated.api.AttributesApi;
import bm.traccar.generated.model.dto.Attribute;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpStatusCodeException;

/** Simple AttributesApi test with explicit HttpStatusCodeException handling. */
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = {ApiService.class})
@TestPropertySource("classpath:application.properties")
@Import({ApiConfig.class, AttributesTestConfig.class})
public class GeneratedAttributesApiIT {

  @Autowired private AttributesApi attributesApi;

  @Value("${traccar.web.serviceAccountToken}")
  private String token;

  @Autowired private ApiService api;

  @Test
  public void attributesComputedGet() {

    api.setBearerToken(token);

    // Boolean | Can only be used by admins or managers to fetch all entities
    Boolean all = true;
    // Integer | Standard users can use this only with their own _userId_
    Integer userId = 56;
    // Integer | Standard users can use this only with _deviceId_s, they have access to
    Integer deviceId = 56;
    // Integer | Standard users can use this only with _groupId_s, they have access to
    Integer groupId = 56;
    Boolean refresh = true;

    List<Attribute> result = null;
    try {
      result = attributesApi.attributesComputedGet(all, userId, deviceId, groupId, refresh);
      System.out.println("result: " + result);
    } catch (HttpStatusCodeException e) {
      System.err.println("Exception when calling AttributesApi#attributesComputedGet");
      System.err.println("Status code: " + e.getStatusCode().value());
      System.err.println("Reason: " + e.getResponseBodyAsString());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
    assertNotNull(result, "request returned null");
  }
}
