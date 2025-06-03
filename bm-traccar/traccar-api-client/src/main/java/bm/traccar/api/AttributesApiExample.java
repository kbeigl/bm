package bm.traccar.api;

import bm.traccar.generated.api.AttributesApi;
import bm.traccar.generated.model.dto.*;
import bm.traccar.invoke.*;
import bm.traccar.invoke.auth.*;
import java.util.List;
import org.springframework.web.client.HttpStatusCodeException;

public class AttributesApiExample {

  // docker:run for testing
  public static void main(String[] args) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("http://localhost");

    // Configure HTTP bearer authorization: ApiKey
    HttpBearerAuth ApiKey = (HttpBearerAuth) apiClient.getAuthentication("ApiKey");
    ApiKey.setBearerToken("BEARER TOKEN");

    // Configure HTTP basic authorization: BasicAuth
    HttpBasicAuth BasicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");
    BasicAuth.setUsername("admin");
    BasicAuth.setPassword("admin");

    System.out.println("bearerToken=" + ApiKey.getBearerToken());

    // ---------------------------------------------------

    AttributesApi apiInstance = new AttributesApi(apiClient);
    Boolean all = true; // Boolean | Can only be used by admins or managers to fetch all entities
    Integer userId = 56; // Integer | Standard users can use this only with their own _userId_
    Integer deviceId =
        56; // Integer | Standard users can use this only with _deviceId_s, they have access to
    Integer groupId =
        56; // Integer | Standard users can use this only with _groupId_s, they have access to
    Boolean refresh = true; // Boolean |
    try {
      List<Attribute> result =
          apiInstance.attributesComputedGet(all, userId, deviceId, groupId, refresh);
      System.out.println("result: " + result);
    } catch (HttpStatusCodeException e) {
      System.err.println("Exception when calling AttributesApi#attributesComputedGet");
      System.err.println("Status code: " + e.getStatusCode().value());
      System.err.println("Reason: " + e.getResponseBodyAsString());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
