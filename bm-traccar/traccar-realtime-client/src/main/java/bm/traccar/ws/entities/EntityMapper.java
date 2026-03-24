package bm.traccar.ws.entities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared, pre-configured ObjectMapper for WebSocket processors JSON handling. */
public final class EntityMapper {
  private static final Logger logger = LoggerFactory.getLogger(EntityMapper.class);

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .addHandler(
              new DeserializationProblemHandler() {
                @Override
                public boolean handleUnknownProperty(
                    DeserializationContext ctxt,
                    JsonParser jp,
                    JsonDeserializer<?> deserializer,
                    Object beanOrClass,
                    String propertyName)
                    throws IOException {
                  logger.warn(
                      "UNIMPLEMENTED unknown field '{}' when deserializing {} - skipping",
                      propertyName,
                      beanOrClass == null ? "<null>" : beanOrClass.getClass().getName());
                  jp.skipChildren();
                  return true; // handled
                }
              });

  private EntityMapper() {}

  public static ObjectMapper get() {
    return MAPPER;
  }
}
