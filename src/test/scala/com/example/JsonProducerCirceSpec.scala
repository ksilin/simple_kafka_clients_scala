package com.example

import com.example.serde.JsonStringProducerCirce
import com.example.util.{ClientSetup, KafkaSpecHelper, SpecBase}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.{Deserializer, Serdes, Serializer}
import org.apache.kafka.common.serialization.Serdes.WrapperSerde
import nequi.circe.kafka._
import io.circe.generic.auto._

import java.lang
import java.nio.charset.{Charset, StandardCharsets}
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters._
import scala.util.Random

class JsonProducerCirceSpec extends SpecBase {

  case class MyRecord(name: String, description: String, timestamp: lang.Long)

  val myRecordSerializer: Serializer[MyRecord] = implicitly
  val myRecordDeserializer: Deserializer[MyRecord] = implicitly
  val myRecordSerde: WrapperSerde[MyRecord] =   new WrapperSerde(myRecordSerializer, myRecordDeserializer)

  val prefix: String = suiteName
  val topic = s"${prefix}_testTopic"
  val cGroup = s"${prefix}_cGroup"

  // assumes local broker accessible on 29092
   "must produce data to CCloud" in {

     //val setup: ClientSetup = ClientSetup(configPath = Some("local"))
     val setup: ClientSetup = ClientSetup(configPath = Some("ccloud.ps.ksilin.dedicated_ksilin"))
     val producer = new JsonStringProducerCirce[String, MyRecord](setup.commonProps, topic)
     setup.commonProps.put(ConsumerConfig.GROUP_ID_CONFIG, cGroup)
     val consumer = new KafkaConsumer[String, MyRecord](setup.commonProps, Serdes.String().deserializer(), myRecordDeserializer)
     consumer.subscribe(List(topic).asJava)

     KafkaSpecHelper.createOrTruncateTopic(setup.adminClient, topic, 1, 3)

     val keys: IndexedSeq[String] = (1 to 3) map (_ => Random.alphanumeric.take(3).mkString)
     val records: List[(String, MyRecord)] = makeRecordTuples(keys, 100)
     val headers: List[(String, Array[Byte])] = List("testHeader" -> "test header data".getBytes(StandardCharsets.UTF_8))

     records foreach { case (k, v) =>
       producer.produce(k, v, headers)
     }
     KafkaSpecHelper.fetchAndProcessRecords(consumer)
   }

  private def makeRecordTuples(keys: Seq[String], howMany: Int, intervalBetweenRecordsMs: Int = 200, startTime: Instant = Instant.now()) = {
    val tuples = (1 to howMany) map { i =>
      val key = Random.shuffle(keys).head
      val description = Random.alphanumeric.take(1).mkString
      val timestamp = startTime.plus(i * intervalBetweenRecordsMs, ChronoUnit.MILLIS).toEpochMilli
      val value = MyRecord(key, description, timestamp)
      (i.toString, value)
    }
    tuples.toList
  }
}
