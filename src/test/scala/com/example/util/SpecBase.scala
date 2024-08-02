package com.example.util

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.{ Deserializer, Serdes }
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.must.Matchers
import wvlet.log.LogSupport

import scala.jdk.CollectionConverters._

class SpecBase extends AnyFreeSpecLike with LogSupport with Matchers {

  def makeTypedConsumer[V](
      setup: ClientConnectionSetup,
      topic: String,
      deserializer: Deserializer[V]
  ): KafkaConsumer[String, V] = {
    val consumer = new KafkaConsumer[String, V](
      setup.commonProps,
      Serdes.String().deserializer(),
      deserializer
    )
    consumer.subscribe(List(topic).asJava)
    consumer
  }

  def makeStringConsumer(
      setup: ClientConnectionSetup,
      topic: String
  ): KafkaConsumer[String, String] = {
    val consumer = new KafkaConsumer[String, String](
      setup.commonProps,
      Serdes.String().deserializer(),
      Serdes.String().deserializer(),
    )
    consumer.subscribe(List(topic).asJava)
    consumer
  }

}
