package bm.gps.gpx.file;

import java.util.Objects;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

/** Copies a single file from a source directory to a target directory via Camel file endpoints. */
public class CopyFileRoute extends RouteBuilder {

  private static final long POLL_TIMEOUT_MS = 10_000L;

  private final String fileName, sourceDirectory, targetDirectory;
  private final String routeId, triggerUri;

  public CopyFileRoute(String fileName, String sourceDirectory, String targetDirectory) {
    this.fileName = Objects.requireNonNull(fileName, "fileName must not be null");
    this.sourceDirectory =
        Objects.requireNonNull(sourceDirectory, "sourceDirectory must not be null");
    this.targetDirectory =
        Objects.requireNonNull(targetDirectory, "inputDirectory must not be null");

    this.routeId = "send-file-route-" + Math.abs(fileName.hashCode());
    this.triggerUri = "direct:" + routeId;
  }

  @Override
  public void configure() {
    from(triggerUri)
        .routeId(routeId)
        .pollEnrich(sourceEndpointUri(), POLL_TIMEOUT_MS)
        .choice()
        .when(body().isNull())
        .throwException(
            new IllegalStateException(
                "Timed out while waiting for file '" + fileName + "' in " + sourceDirectory))
        .end()
        .to(targetEndpointUri());
  }

  public void copyAndAwait(CamelContext camel, ProducerTemplate producer) throws Exception {
    camel.addRoutes(this);
    try {
      // direct endpoints are synchronous; sendBody returns after the file copy completes.
      producer.sendBody(triggerUri, null);
    } finally {
      if (camel.getRoute(routeId) != null) {
        camel.getRouteController().stopRoute(routeId);
        camel.removeRoute(routeId);
      }
    }
  }

  private String sourceEndpointUri() {
    return String.format(
        "file:%s?noop=true&idempotent=false&readLock=none&fileName=%s", sourceDirectory, fileName);
  }

  private String targetEndpointUri() {
    return String.format("file:%s?autoCreate=true&fileName=%s", targetDirectory, fileName);
  }
}
