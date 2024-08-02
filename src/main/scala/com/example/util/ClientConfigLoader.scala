package com.example.util

import com.typesafe.config.{ Config, ConfigFactory, ConfigValueFactory }

import java.util.Properties

object ClientConfigLoader {

  def loadConfig(profile: String): Config = {
    val baseConfig = ConfigFactory.load()

    val commonConfig = baseConfig.getConfig("client.common")

    val producerBaseConfig = baseConfig.getConfig("client.producer.base").withFallback(commonConfig)
    val consumerBaseConfig = baseConfig.getConfig("client.consumer.base").withFallback(commonConfig)

    val producerProfileConfig =
      baseConfig.getConfig(s"client.producer.profile.$profile").withFallback(producerBaseConfig)
    val consumerProfileConfig =
      baseConfig.getConfig(s"client.consumer.profile.$profile").withFallback(consumerBaseConfig)

    val finalConfig = baseConfig
      .withValue("client.producer", producerProfileConfig.root())
      .withValue("client.consumer", consumerProfileConfig.root())

    applyEnvOverrides(finalConfig).resolve()
  }

  def applyEnvOverrides(config: Config): Config = {
    val envVars = sys.env
    val configWithOverrides = envVars.foldLeft(config) { (accConfig, envVar) =>
      val (envKey, envValue) = envVar
      val configKey = sanitizeConfigKey(envKey.toLowerCase.replaceAll("_", "."))
      if (accConfig.hasPath(configKey)) {
        accConfig.withValue(configKey, ConfigValueFactory.fromAnyRef(envValue))
      } else {
        accConfig
      }
    }
    configWithOverrides
  }

  def sanitizeConfigKey(key: String): String = {
    key.replaceAll("\\.{2,}", ".").stripPrefix(".").stripSuffix(".")
  }

  def getProducerConfig(config: Config): Config =
    config.getConfig("client.producer")

  def getConsumerConfig(config: Config): Config =
    config.getConfig("client.consumer")

  def getProps(config: Config): Properties = {
    val props = new Properties()
    config.entrySet().forEach { entry =>
      props.put(entry.getKey, entry.getValue.unwrapped().toString)
    }
    props
  }

}
