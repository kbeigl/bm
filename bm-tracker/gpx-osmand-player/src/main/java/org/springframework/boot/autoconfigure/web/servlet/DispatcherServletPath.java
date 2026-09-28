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
package org.springframework.boot.autoconfigure.web.servlet;

/**
 * Compatibility bridge for Hawtio with Spring Boot 4.
 *
 * <p>Spring Boot 4 reorganized web servlet paths. This interface provides the legacy contract
 * expected by Hawtio's auto-configuration and endpoint path resolver.
 */
@FunctionalInterface
public interface DispatcherServletPath {

  String getPath();

  default String getRelativePath(String path) {
    String prefix = getPrefix();
    if (!path.startsWith(prefix)) {
      return path;
    }
    String relativePath = path.substring(prefix.length());
    return relativePath.startsWith("/") ? relativePath : "/" + relativePath;
  }

  default String getPrefix() {
    String path = getPath();
    int index = path.lastIndexOf('/');
    return (index != -1) ? path.substring(0, index) : "";
  }

  default String getServletUrlMapping() {
    return getPath();
  }
}
