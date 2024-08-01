package com.example

import com.example.util.SpecBase
import org.apache.kafka.clients.consumer.{
  ConsumerRecord,
  ConsumerRecords,
  MockConsumer,
  OffsetResetStrategy
}
import org.apache.kafka.common.TopicPartition

import java.lang
import java.time.Duration
import java.util.concurrent.{ Executors, ScheduledExecutorService, TimeUnit }
import scala.jdk.CollectionConverters._
import scala.util.Try

class MockConsumerSpec extends SpecBase {

  val prefix: String = suiteName
  val topic          = s"${prefix}_testTopic"

  "must produce and consume data with mock clients" in {

    val consumer: MockConsumer[String, String] =
      new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    val tp: TopicPartition = new TopicPartition(topic, 0)

    consumer.subscribe(List(topic).asJava)

    consumer.schedulePollTask { () =>
      consumer.rebalance(List(tp).asJava)
      (0 to 100) foreach { i =>
        val consumerRecord = new ConsumerRecord[String, String](topic, 0, i, s"key_$i", s"value_$i")
        consumer.addRecord(consumerRecord)
      }
    }

    val startOffsets: Map[TopicPartition, lang.Long] = Map(tp -> 0L)
    consumer.updateBeginningOffsets(startOffsets.asJava)

    val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    val consumerTask: Runnable = () => {
      while (!scheduler.isShutdown) {
        Thread.sleep(100)
        println("polling...")
        val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofMillis(10))
        records.iterator().asScala foreach { r =>
          println(s"processing record $r")
        }
      }

      logger warn s"Closing the consumer now"
      Try(consumer.close())
        .recover { case error => logger.error("Failed to close the kafka consumer", error) }
    }

    scheduler.schedule(consumerTask, 1L, TimeUnit.MILLISECONDS)
    val stopConsumer: Runnable = () => consumer.wakeup()
    scheduler.schedule(stopConsumer, 1000L, TimeUnit.MILLISECONDS)
    Thread.sleep(100)
    println("test concluded")

  }

}
