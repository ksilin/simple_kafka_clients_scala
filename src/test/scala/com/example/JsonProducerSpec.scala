package com.example

import com.example.serde.{ GsonDeserializer, GsonSerializer }
import com.example.util.FutureConverter.toScalaFuture
import com.example.util.{ ClientConnectionSetup, KafkaSpecHelper, SpecBase }
import org.apache.kafka.clients.consumer.{ ConsumerConfig, KafkaConsumer }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord, RecordMetadata }
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serdes.WrapperSerde

import java.lang
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Random

class JsonProducerSpec extends SpecBase {

  case class MyRecord(name: String, description: String, timestamp: lang.Long)

  val myRecordSerializer   = new GsonSerializer[MyRecord]
  val myRecordDeserializer = new GsonDeserializer[MyRecord](classOf[MyRecord])
  val myRecordSerde: WrapperSerde[MyRecord] =
    new WrapperSerde(myRecordSerializer, myRecordDeserializer)

  val prefix: String = suiteName
  val topic          = s"${prefix}_testTopic"
  val cGroup         = s"${prefix}_cGroup"

  // assumes local broker accessible on 29092
  "must produce data locally" in {

    val setup: ClientConnectionSetup = ClientConnectionSetup(configPath = Some("local"))
    val producer = new KafkaProducer[String, MyRecord](
      setup.commonProps,
      Serdes.String().serializer(),
      myRecordSerializer
    )
    setup.commonProps.put(ConsumerConfig.GROUP_ID_CONFIG, cGroup)
    val consumer = new KafkaConsumer[String, MyRecord](
      setup.commonProps,
      Serdes.String().deserializer(),
      myRecordDeserializer
    )
    consumer.subscribe(List(topic).asJava)

    KafkaSpecHelper.createOrTruncateTopic(setup.adminClient, topic, 1, 1)

    val keys: IndexedSeq[String] = (1 to 3) map (_ => Random.alphanumeric.take(3).mkString)
    val records: List[ProducerRecord[String, MyRecord]] = makeRecords(keys, 100)

    val sent: Seq[Future[RecordMetadata]] = records map { r =>
      toScalaFuture(producer.send(r))
    }
    val singeSent = Future.sequence(sent)
    Await.result(singeSent, 60.seconds)
    KafkaSpecHelper.fetchAndProcessRecords(consumer)
  }

  // CREATE STREAM collector (name String, description String, timestamp BIGINT) WITH (kafka_topic = 'JsonProducerSpec_testTopic', value_format = 'JSON');
  "must produce data to CCloud" in {

    val setup: ClientConnectionSetup =
      ClientConnectionSetup(configPath = Some("ccloud.ps.ksilin.dedicated_ksilin"))
    val producer = new KafkaProducer[String, MyRecord](
      setup.commonProps,
      Serdes.String().serializer(),
      myRecordSerializer
    )
    setup.commonProps.put(ConsumerConfig.GROUP_ID_CONFIG, cGroup)
    val consumer = new KafkaConsumer[String, MyRecord](
      setup.commonProps,
      Serdes.String().deserializer(),
      myRecordDeserializer
    )
    consumer.subscribe(List(topic).asJava)

    KafkaSpecHelper.createOrTruncateTopic(setup.adminClient, topic, 1)

    val keys: IndexedSeq[String] = (1 to 3) map (_ => Random.alphanumeric.take(3).mkString)
    val records: List[ProducerRecord[String, MyRecord]] = makeRecords(keys, 2000)

    val sent: Seq[Future[RecordMetadata]] = records map { r =>
      toScalaFuture(producer.send(r))
    }
    val singeSent = Future.sequence(sent)
    Await.result(singeSent, 60.seconds)
    KafkaSpecHelper.fetchAndProcessRecords(consumer)
  }

  private def makeRecords(
      keys: Seq[String],
      howMany: Int,
      intervalBetweenRecordsMs: Int = 200,
      startTime: Instant = Instant.now()
  ) = {
    val records = (1 to howMany) map { i =>
      val key         = Random.shuffle(keys).head
      val description = Random.alphanumeric.take(1).mkString
      val timestamp   = startTime.plus(i * intervalBetweenRecordsMs, ChronoUnit.MILLIS).toEpochMilli
      val value       = MyRecord(key, description, timestamp)
      val record = new ProducerRecord[String, MyRecord](topic, null, timestamp, i.toString, value)
      record
    }
    records.toList
  }
}
