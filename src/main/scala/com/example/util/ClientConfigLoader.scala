package com.example.util

import com.typesafe.config.{Config, ConfigFactory}

import java.util.Properties

object ClientConfigLoader {

  def loadConfig(profile: String): Config = {
    val baseConfig = ConfigFactory.load()

    val commonConfig = baseConfig.getConfig("client.common")

    val producerBaseConfig = baseConfig.getConfig("client.producer.base").withFallback(commonConfig)
    val consumerBaseConfig = baseConfig.getConfig("client.consumer.base").withFallback(commonConfig)

    val producerProfileConfig = baseConfig.getConfig(s"client.producer.profile.$profile").withFallback(producerBaseConfig)
    val consumerProfileConfig = baseConfig.getConfig(s"client.consumer.profile.$profile").withFallback(consumerBaseConfig)

    val finalProducerConfig = producerProfileConfig.resolve()
    val finalConsumerConfig = consumerProfileConfig.resolve()

    baseConfig
      .withValue("client.producer", finalProducerConfig.root())
      .withValue("client.consumer", finalConsumerConfig.root())
      .resolve()
  }

  def getProducerConfig(config: Config): Config = {
    config.getConfig("client.producer")
  }

  def getConsumerConfig(config: Config): Config = {
    config.getConfig("client.consumer")
  }

  def getProps(config: Config): Properties = {
    val props = new Properties()
    config.entrySet().forEach { entry =>
      props.put(entry.getKey, entry.getValue.unwrapped().toString)
    }
    props
  }

}
