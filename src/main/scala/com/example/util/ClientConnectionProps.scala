/*
 * Copyright 2022 ksilin
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

package com.example.util

import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.{ SaslConfigs, SslConfigs }

import java.net.URL
import java.util.Properties

case class ClientConnectionProps(
    bootstrapServer: String,
    apiKey: Option[String],
    apiSecret: Option[String],
    principal: Option[String]
) {

  val clientProps: Properties = new Properties()
  clientProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
  clientProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  apiKey zip apiSecret foreach {
    case (k, s) =>
      clientProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https")
      clientProps.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
      clientProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN")

      val saslString: String =
        s"""org.apache.kafka.common.security.plain.PlainLoginModule required username="${k}" password="${s}";""".stripMargin
      clientProps.setProperty(SaslConfigs.SASL_JAAS_CONFIG, saslString)
  }
}

case object ClientConnectionProps {

  val prefix = "kafka"

  def create(
      configFileUrl: Option[URL] = None,
      configPath: Option[String] = None
  ): ClientConnectionProps = {
    val topLevelConfig = configFileUrl.fold(ConfigFactory.load())(ConfigFactory.parseURL)
    val config = configPath.map(path => topLevelConfig.getConfig(path)).getOrElse(topLevelConfig)
    fromConfig(config)
  }

  def fromConfig(config: Config): ClientConnectionProps =
    ClientConnectionProps(
      config.getString(s"${prefix}.bootstrap"),
      if (config.hasPath(s"${prefix}.key")) Some(config.getString(s"${prefix}.key")) else None,
      if (config.hasPath(s"${prefix}.secret")) Some(config.getString(s"${prefix}.secret"))
      else None,
      if (config.hasPath(s"${prefix}.principal")) Some(config.getString(s"${prefix}.principal"))
      else None
    )
}
