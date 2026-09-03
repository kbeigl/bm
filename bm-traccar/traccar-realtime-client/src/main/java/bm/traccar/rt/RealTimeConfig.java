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
package bm.traccar.rt;

import bm.traccar.api.ApiConfig;
import bm.traccar.api.scenario.ScenarioConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Main configuration for the traccar-realtime-client module.
 *
 * <p>This config lives in main sources so other modules can import it without depending on the
 * {@code @SpringBootApplication} class.
 *
 * <p>When using this library, simply import this configuration:
 *
 * <pre>{@code
 * @SpringBootApplication
 * @Import(RealTimeConfig.class)
 * public class MyApp { }
 * }</pre>
 */
@Configuration
@ComponentScan(basePackages = {"bm.traccar", "bm.traccar.rt", "bm.traccar.ws"})
@Import({ApiConfig.class, ScenarioConfig.class}) // Import dependency configs
public class RealTimeConfig {}
