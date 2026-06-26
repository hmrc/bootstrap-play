/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.play.bootstrap.filters

import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker

/**
 * Suppresses Play's startup warning:
 *   "Logger configuration in conf files is deprecated and has no effect.
 *    Use a logback configuration file instead."
 *
 * Play emits this (in GuiceApplicationBuilder) whenever it finds log level
 * values under the `logger` config path, on the assumption that nothing
 * consumes them. On MDTP that assumption is false: `LoggerModule` reads those
 * same `logger.*` entries at startup and applies them via Logback's
 * `setLevel`, so config-driven log levels DO take effect.
 *
 * This must be a Logback TurboFilter rather than logic in `LoggerModule`,
 * because the warning is emitted during logger-factory configuration —
 * before module bindings run — so by the time `LoggerModule` executes the
 * message has already been logged. A TurboFilter is loaded with the Logback
 * config (before the warning fires) and is consulted for every logging event,
 * so it can drop this specific message at source.
 */
class SuppressLoggerDeprecationFilter extends TurboFilter {
  override def decide(
    marker: Marker
  , logger: Logger
  , level : Level
  , format: String
  , params: Array[AnyRef]
  , t     : Throwable
  ): FilterReply =
    if (format != null && format.contains("Logger configuration in conf files is deprecated"))
      FilterReply.DENY
    else
      FilterReply.NEUTRAL
}
