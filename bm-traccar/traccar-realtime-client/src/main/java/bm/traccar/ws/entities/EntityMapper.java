/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                  logger.debug( // warn ?
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
