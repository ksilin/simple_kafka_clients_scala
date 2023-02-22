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

  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

  private val cliOptions: HeaderBytesPrinterOptions = new HeaderBytesPrinterOptions()

  // while you can pass java array to varargs method, you need to splat a scala array
  new CommandLine(cliOptions).parseArgs(args: _*)
  // println(cliOptions)

  private val configFileName = cliOptions.configFile
  val topicName      = cliOptions.topic

  private val overrideProps: Properties = buildProperties(configFileName)

  private val consumerProps = new Properties()
  consumerProps.putAll(overrideProps)

  val listener = new ConsumerRebalanceListener {
    override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit =
      logger info s"The following partition are revoked: ${partitions.asScala.mkString(", ")}"

    override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit =
      logger info s"The following partition are assigned: ${partitions.asScala.mkString(", ")}"
  }


  private val runnable: Runnable = () => {

    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](
      consumerProps,
      Serdes.ByteArray().deserializer(),
      Serdes.ByteArray().deserializer()
    )
    consumer.subscribe((topicName :: Nil).asJava, listener)

    while (!scheduler.isShutdown) {
      Thread.sleep(100)
      println("polling...")
      val records: ConsumerRecords[Array[Byte], Array[Byte]] = consumer.poll(Duration.ofSeconds(1))
      val r: List[ConsumerRecord[Array[Byte], Array[Byte]]]  = records.asScala.toList
      println(s"received ${r.length} records")
      for (record: ConsumerRecord[Array[Byte], Array[Byte]] <- r) {
        val partition = record.partition()
        val offset    = record.offset()
        val key       = record.key().mkString
        println(s"headers for $partition:$offset:$key")
        val headerStrings =
          record
            .headers()
            .asScala
            .map(h => s"${h.key()} : ${new String(h.value(), StandardCharsets.UTF_8)}")
            .mkString("\n")
        println(headerStrings)
        if (cliOptions.printDecimalBytes) {
          println("decimal bytes:")
          val headerBytes =
            record
              .headers()
              .asScala
              .map(h => s"${h.key()} : ${h.value().mkString(" ")}")
              .mkString("\n")
          println(headerBytes)
        }
        if (cliOptions.printHexBytes) {
          println("hex bytes:")
          val headerHex2 =
            record
              .headers()
              .asScala
              .map(h => s"${h.key()} : ${toHex(h.value())}")
              .mkString("\n")
          println(headerHex2)
        }
        println("---")
      }
    }
    logger warn s"Closing the consumer now"
    Try(consumer.close())
      .recover { case error => logger.error("Failed to close the kafka consumer", error) }
  }

  scheduler.schedule(runnable, 1, TimeUnit.SECONDS)

  sys.addShutdownHook {
    scheduler.shutdown()
    scheduler.awaitTermination(10, TimeUnit.SECONDS)
  }

  private def toHex(bytes: Seq[Byte]): String =
    (bytes map { b =>
      String.format("%02x", b)
    }).mkString(" ")

  private def buildProperties(configFileName: String): Properties = {
    val properties = new Properties()
    properties.put(
      ConsumerConfig.GROUP_ID_CONFIG,
      "header_bytes_printer_group_" + Random.alphanumeric.take(10).mkString
    )
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    properties.load(new FileReader(configFileName))
    properties
  }

}
