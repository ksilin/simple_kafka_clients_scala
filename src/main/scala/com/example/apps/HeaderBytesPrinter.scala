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

package com.example.apps

import org.apache.kafka.clients.consumer.{
  ConsumerConfig,
  ConsumerRebalanceListener,
  ConsumerRecord,
  ConsumerRecords,
  KafkaConsumer
}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serdes
import picocli.CommandLine
import wvlet.log.LogSupport

import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.{ Executors, ScheduledExecutorService, TimeUnit }
import java.util
import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.util.{ Random, Try }

case object HeaderBytesPrinter extends App with LogSupport {

  val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

  val cliOptions: HeaderBytesPrinterOptions = new HeaderBytesPrinterOptions()

  // while you can pass java array to varargs method, you need to splat a scala array
  new CommandLine(cliOptions).parseArgs(args: _*)
  println("options:")
  println(cliOptions)

  val configFileName = cliOptions.configFile
  val topicName      = cliOptions.topic

  val overrideProps: Properties = buildProperties(configFileName)

  // val setup: ClientSetup = ClientSetup(configPath = Some("ccloud.ps.ksilin.dedicated_ksilin"))
  val consumerProps = new Properties()
  // consumerProps.putAll(setup.commonProps)
  consumerProps.putAll(overrideProps)

  private val runnable: Runnable = () => {
    var map = Map.empty[String, Int]
    val listener = new ConsumerRebalanceListener {
      override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit =
        logger info s"The following partition are revoked: ${partitions.asScala.mkString(", ")}"

      override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit =
        logger info s"The following partition are assigned: ${partitions.asScala.mkString(", ")}"
    }

    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](
      consumerProps,
      Serdes.ByteArray().deserializer(),
      Serdes.ByteArray().deserializer()
    )
    consumer.subscribe((topicName :: Nil).asJava, listener)

    while (!scheduler.isShutdown) {
      Thread.sleep(2000)
      println("polling...")
      val records: ConsumerRecords[Array[Byte], Array[Byte]] = consumer.poll(Duration.ofSeconds(1))
      val r: List[ConsumerRecord[Array[Byte], Array[Byte]]]  = records.asScala.toList
      println(s"received ${r.length} records")
      for (record: ConsumerRecord[Array[Byte], Array[Byte]] <- r) {
        val partition = record.partition()
        val offset    = record.offset()
        val key       = record.key().mkString
        val headerBytes =
          record.headers().asScala.map(h => s"${h.key()}:${h.value().mkString(",")}").mkString("\n")
        println(s"headers for $partition:$offset:$key:")
        println(headerBytes)
        if (cliOptions.printStrings) {
          val headerStrings =
            record
              .headers()
              .asScala
              .map(h => s"${h.key()}:${new String(h.value(), StandardCharsets.UTF_8)}")
              .mkString("\n")
          println(headerStrings)
        }
        println("---")
      }
    }

    println()
    logger warn s"Closing the the first consumer n°2 now!"
    Try(consumer.close())
      .recover { case error => logger.error("Failed to close the kafka consumer", error) }
  }

  scheduler.schedule(runnable, 1, TimeUnit.SECONDS)

  sys.addShutdownHook {
    scheduler.shutdown()
    scheduler.awaitTermination(10, TimeUnit.SECONDS)
  }

  def buildProperties(configFileName: String): Properties = {
    val properties = new Properties()
    properties.put(
      ConsumerConfig.GROUP_ID_CONFIG,
      "scala_example_group_" + Random.alphanumeric.take(5).mkString
    )
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    properties.load(new FileReader(configFileName))
    properties
  }

}
