package com.example

import com.example.serde.JsonStringProducerCirce
import com.example.util.{ ClientConnectionSetup, KafkaSpecHelper, SpecBase }
import org.apache.kafka.clients.consumer.{ ConsumerConfig, KafkaConsumer }
import org.apache.kafka.common.serialization.{ Deserializer, Serializer }
import org.apache.kafka.common.serialization.Serdes.WrapperSerde
import nequi.circe.kafka._
import io.circe.generic.auto._

import java.lang
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.util.Random

class JsonStringProducerCirceSpec extends SpecBase {

  case class MyRecord(name: String, description: String, timestamp: lang.Long)

  val myRecordSerializer: Serializer[MyRecord]     = implicitly
  val myRecordDeserializer: Deserializer[MyRecord] = implicitly
  val myRecordSerde: WrapperSerde[MyRecord] =
    new WrapperSerde(myRecordSerializer, myRecordDeserializer)

  val prefix: String = suiteName
  val topic          = s"jsonTest" // "${prefix}_testTopic"
  val cGroup         = s"${prefix}_cGroup"

  "must produce data to CCloud" in {

    // val setup: ClientSetup = ClientSetup(configPath = Some("local"))
    val setup: ClientConnectionSetup = ClientConnectionSetup(configPath = Some("ccloud.ps.ksilin.dedicated_ksilin"))
    val producer           = new JsonStringProducerCirce[String, MyRecord](setup.commonProps, topic)

    setup.commonProps.put(
      ConsumerConfig.GROUP_ID_CONFIG,
      cGroup + Random.alphanumeric.take(3).mkString
    )
    val consumer: KafkaConsumer[String, MyRecord] =
      makeTypedConsumer(setup, topic, myRecordDeserializer)
    setup.commonProps.put(
      ConsumerConfig.GROUP_ID_CONFIG,
      cGroup + Random.alphanumeric.take(3).mkString
    )
    val stringConsumer: KafkaConsumer[String, String] = makeStringConsumer(setup, topic)

    KafkaSpecHelper.createOrTruncateTopic(setup.adminClient, topic, 1, 3)

    val keys: IndexedSeq[String]          = (1 to 3) map (_ => Random.alphanumeric.take(3).mkString)
    val records: List[(String, MyRecord)] = makeRecordTuples(keys, 100)
    val headers: List[(String, Array[Byte])] =
      List("testHeader" -> "test header data".getBytes(StandardCharsets.UTF_8))

    records foreach {
      case (k, v) =>
        producer.produce(k, v, headers)
    }
    KafkaSpecHelper.fetchAndProcessRecords(consumer)
    KafkaSpecHelper.fetchAndProcessRecords(stringConsumer)
  }

  "must be able to deal with Options" in {}

  "must be able to deal with Either" in {}

  "must be able to deal with DateTime" in {}

  private def makeRecordTuples(
      keys: Seq[String],
      howMany: Int,
      intervalBetweenRecordsMs: Int = 200,
      startTime: Instant = Instant.now()
  ) = {
    val tuples = (1 to howMany) map { i =>
      val key         = Random.shuffle(keys).head
      val description = Random.alphanumeric.take(1).mkString
      val timestamp   = startTime.plus(i * intervalBetweenRecordsMs, ChronoUnit.MILLIS).toEpochMilli
      val value       = MyRecord(key, description, timestamp)
      (i.toString, value)
    }
    tuples.toList
  }
}
