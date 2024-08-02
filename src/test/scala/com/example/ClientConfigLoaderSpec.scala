package com.example

import com.example.util.{ ClientConfigLoader, SpecBase }
import scala.jdk.CollectionConverters._

class ClientConfigLoaderSpec extends SpecBase {

  "must load configs for producers and consumers according to given profile" in {

    val profile        = sys.env.getOrElse("PROFILE", "prod") // default to "local"
    val config         = ClientConfigLoader.loadConfig(profile)
    val producerConfig = ClientConfigLoader.getProducerConfig(config)
    val consumerConfig = ClientConfigLoader.getConsumerConfig(config)

    logger.info(ClientConfigLoader.getProps(producerConfig).asScala.toMap.mkString("\n"))
    logger.info(ClientConfigLoader.getProps(consumerConfig).asScala.toMap.mkString("\n"))

  }

}
