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

package com.example.serde
import java.util.Properties
import org.apache.kafka.clients.producer.{
  KafkaProducer,
  ProducerConfig,
  ProducerRecord,
  RecordMetadata
}
import io.circe._
import io.circe.syntax._
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.StringSerializer
import wvlet.log.LogSupport

import java.util.concurrent.Future

case class JsonStringProducerCirce[K, V](
    clientProperties: Properties,
    topic: String = "testTopic",
    clientId: String = "JsonStringProducerCirce"
)(implicit e: Encoder[V])
    extends LogSupport {

  private val producerProperties = new Properties()
  producerProperties.putAll(clientProperties)
  producerProperties.put(ProducerConfig.ACKS_CONFIG, "all")
  producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
  producerProperties.put(
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
    classOf[StringSerializer]
  ) // so, not really K
  producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
  producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId)

  val producer = new KafkaProducer[K, String](producerProperties)

  def makeRecords(
      recordMap: Iterable[(K, V)],
      headers: Iterable[(String, Array[Byte])] = Nil
  ): Iterable[ProducerRecord[K, String]] =
    recordMap.map { case (k, v) => makeRecord(k, v, headers) }

  def makeRecord(
      key: K,
      value: V,
      headerData: Iterable[(String, Array[Byte])] = Nil
  ): ProducerRecord[K, String] = {
    val v                = if (null == value) null else value.asJson.noSpaces
    val r                = new ProducerRecord[K, String](topic, key, v)
    val headers: Headers = r.headers()
    headerData foreach {
      case (name, value) =>
        headers.add(name, value)
    }
    r
  }

  def produceString(r: ProducerRecord[K, String]): Unit = {
    info(s"producing $r")
    val res: RecordMetadata = producer.send(r).get
    info(s"produced ${res.topic()}, | ${res.partition()} | ${res.offset()} | ${res.timestamp()}")
  }

  def produce(key: K, value: V, headers: Iterable[(String, Array[Byte])] = Nil): Unit = {
    val res: RecordMetadata = produceAsync(key, value, headers).get
    info(s"produced ${res.topic()}, | ${res.partition()} | ${res.offset()} | ${res.timestamp()}")
  }

  def produceAsync(
      key: K,
      value: V,
      headers: Iterable[(String, Array[Byte])] = Nil
  ): Future[RecordMetadata] = {
    val r = makeRecord(key, value, headers)
    info(s"producing $r")
    producer.send(r) // ((recordMetadata, exception)
  }

  def produceString(msgs: Iterable[ProducerRecord[K, String]], sendDelayMs: Int = 0): Unit =
    msgs foreach { r =>
      produceString(r)
      Thread.sleep(sendDelayMs)
    }

  def produce(msgs: Iterable[(K, V, Iterable[(String, Array[Byte])])]): Unit =
    msgs foreach { r =>
      produce(r._1, r._2, r._3)
    }
}
