package com.example

import com.example.serde.{GsonDeserializer, GsonSerializer}
import com.example.util.FutureConverter.toScalaFuture
import com.example.util.{KafkaSpecHelper, SpecBase}
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serdes.WrapperSerde

import java.lang
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Properties
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

class JsonProducerSpec extends SpecBase {

  case class MyRecord(name: String, description: String, timestamp: lang.Long)

  val jsonSerializer = new GsonSerializer[MyRecord]
  val jsonDeserializer = new GsonDeserializer[MyRecord](classOf[MyRecord])
  val myRecordSerde: WrapperSerde[MyRecord] =   new WrapperSerde(jsonSerializer, jsonDeserializer)

  val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092")

  val topic = "testTopic"

  val adminClient = AdminClient.create(props)

  val producer = new KafkaProducer[String, MyRecord](props, Serdes.String().serializer(), jsonSerializer)

   "must produce data" in {

     KafkaSpecHelper.createOrTruncateTopic(adminClient, topic, 1, 1)

     val now: Instant = Instant.now()
     val intervalBetweenRecords = 200

     val keys: IndexedSeq[String] = (1 to 3) map (_ => Random.alphanumeric.take(3).mkString)

     val records = (1 to 100) map { i =>
       val key = Random.shuffle(keys).head
       val description = Random.alphanumeric.take(1).mkString
       val timestamp = now.plus(i * intervalBetweenRecords, ChronoUnit.MILLIS).toEpochMilli
       val value = MyRecord(key, description, timestamp)
       val record = new ProducerRecord[String, MyRecord](topic, null, timestamp, i.toString, value)
       record
     }

     val sent: Seq[Future[RecordMetadata]] = records map { r =>



       toScalaFuture(producer.send(r))
     }
     val singeSent = Future.sequence(sent)
     Await.result(singeSent, 60.seconds)
   }

}
