package com.example.util

import com.example.util.FutureConverter.toScalaFuture
import org.apache.kafka.clients.admin.{AdminClient, CreateTopicsResult, DeleteTopicsResult, NewTopic}
import org.apache.kafka.clients.consumer.{Consumer, ConsumerRecord, ConsumerRecords}
import org.apache.kafka.common.config.TopicConfig
import wvlet.log.LogSupport

import java.time
import java.util.Collections
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.jdk.javaapi.CollectionConverters.asJava
import scala.util.Try

object KafkaSpecHelper extends LogSupport with FutureConverter {

  val metadataWait             = 2000
  val defaultReplicationFactor = 3 // 3 for cloud, 1 for local would make sense

  def createTopic(
      adminClient: AdminClient,
      topicName: String,
      numberOfPartitions: Int = 1,
      replicationFactor: Short = 3,
      skipExistanceCheck: Boolean = false,
      timeoutMs: Long
  ): Either[String, String] = {
    val needsCreation = skipExistanceCheck || !doesTopicExist(adminClient, topicName)
    if (needsCreation) {
      info(s"Creating topic ${topicName}")

      val configs: Map[String, String] =
        if (replicationFactor < 3) Map(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG -> "1")
        else Map.empty

      val newTopic: NewTopic = new NewTopic(topicName, numberOfPartitions, replicationFactor)
      newTopic.configs(configs.asJava)
      try {
        val topicsCreationResult: CreateTopicsResult =
          adminClient.createTopics(Collections.singleton(newTopic))
        val resF = toScalaFuture(topicsCreationResult.all())
        Await.result(resF, timeoutMs.millis)
        info(s"successfully created topic $topicName")
        Right(s"successfully created topic $topicName")
      } catch {
        case e: Throwable =>
          info(s"failed to create topic $topicName: $e")
          Left(e.getMessage)
      }
    } else {
      val topicExistsMsg = s"topic $topicName already exists, skipping"
      info(topicExistsMsg)
      Left(topicExistsMsg)
    }
  }

  def createOrTruncateTopic(
      adminClient: AdminClient,
      topicName: String,
      numberOfPartitions: Int = 1,
      replicationFactor: Short = 3,
      timeoutMs: Long = 10000
  ): Any =
    if (doesTopicExist(adminClient, topicName)) {
      println(s"truncating topic $topicName")
      info(s"truncating topic $topicName")
      truncateTopic(adminClient, topicName, numberOfPartitions, replicationFactor)
    } else {
      println(s"creating topic $topicName")
      info(s"creating topic $topicName")
      createTopic(
        adminClient,
        topicName,
        numberOfPartitions,
        replicationFactor,
        skipExistanceCheck = true,
        timeoutMs
      )
    }

  def deleteTopic(adminClient: AdminClient, topicName: String): Any = {
    debug(s"deleting topic $topicName")
    try {
      val topicDeletionResult: DeleteTopicsResult = adminClient.deleteTopics(List(topicName).asJava)
      topicDeletionResult.all().get()
    } catch {
      case e: Throwable => debug(e)
    }
  }

  // TODO - eliminate waiting by catching the exceptions on retry
  val truncateTopic: (AdminClient, String, Int, Short) => Unit =
    (adminClient: AdminClient, topic: String, partitions: Int, replicationFacor: Short) => {

      val javaTopicSet = asJava(Set(topic))
      logger.info(s"deleting topic $topic")
      val deleted: Try[Void] = Try {
        Await.result(adminClient.deleteTopics(javaTopicSet).all().toScalaFuture, 10.seconds)
      }
      waitForTopicToBeDeleted(adminClient, topic)

      Thread.sleep(metadataWait)
      logger.info(s"creating topic $topic")
      val created: Try[Void] = Try {
        val newTopic =
          new NewTopic(
            topic,
            partitions,
            replicationFacor
          )
        val createTopicsResult: CreateTopicsResult = adminClient.createTopics(asJava(Set(newTopic)))
        Await.result(createTopicsResult.all().toScalaFuture, 10.seconds)
      }
      waitForTopicToExist(adminClient, topic)
      Thread.sleep(metadataWait)
    }

  val waitForTopicToExist: (AdminClient, String) => Unit =
    (adminClient: AdminClient, topic: String) => {
      var topicExists = false
      while (!topicExists) {
        Thread.sleep(100)
        topicExists = doesTopicExist(adminClient, topic)
        if (!topicExists) logger.info(s"topic $topic still does not exist")
      }
    }

  val waitForTopicToBeDeleted: (AdminClient, String) => Unit =
    (adminClient: AdminClient, topic: String) => {
      var topicExists = true
      while (topicExists) {
        Thread.sleep(100)
        topicExists = doesTopicExist(adminClient, topic)
        if (topicExists) logger.info(s"topic $topic still exists")
      }
    }

  val doesTopicExist: (AdminClient, String) => Boolean =
    (adminClient: AdminClient, topic: String) => {
      val names = Await.result(adminClient.listTopics().names().toScalaFuture, 10.seconds)
      names.contains(topic)
    }

  // assumes consumer is already subscribed
  def fetchAndProcessRecords[K, V](
      consumer: Consumer[K, V],
      process: ConsumerRecord[K, V] => Unit = { r: ConsumerRecord[K, V] =>
        if (r.value() == null) {
          info(s"tombstone for key ${r.key()}")
        } else {

          info(
            s"${r.topic()} | ${r.partition()} | ${r.offset()} : ${r.value().getClass} | ${r
              .key()} | ${r.value()} | ${r.headers().asScala.mkString("|")}"
          )
        }
      },
      filter: ConsumerRecord[K, V] => Boolean = { _: ConsumerRecord[K, V] => true },
      abortOnFirstRecord: Boolean = true,
      maxAttempts: Int = 100,
      pause: Int = 100
  ): Iterable[ConsumerRecord[K, V]] = {
    val duration: time.Duration                    = java.time.Duration.ofMillis(100)
    var found                                      = false
    var records: Iterable[ConsumerRecord[K, V]]    = Nil
    var allRecords: Iterable[ConsumerRecord[K, V]] = Nil
    var attempts                                   = 0
    while (!found && attempts < maxAttempts) {
      val consumerRecords: ConsumerRecords[K, V] = consumer.poll(duration)

      attempts = attempts + 1
      found = !consumerRecords.isEmpty && abortOnFirstRecord
      if (!consumerRecords.isEmpty) {
        info(s"fetched ${consumerRecords.count()} records on attempt $attempts")
        records = consumerRecords.asScala.filter(filter)
        allRecords ++= records
        records foreach { r => process(r) }
      }
      Thread.sleep(pause)
    }
    if (attempts >= maxAttempts)
      info(s"${allRecords.size} records received in $attempts attempts")
    allRecords
  }

}
