package com.example

import com.acme.{ Myrecord, Other }
import com.example.util.SpecBase
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.protobuf.{
  KafkaProtobufDeserializer,
  KafkaProtobufDeserializerConfig,
  KafkaProtobufSerializer,
}

import scala.jdk.CollectionConverters._

class SrProtoSerdeSpec extends SpecBase {

  val mockSRUrl = "mock://localhost:8081"

  val dummyTopicName = "dummy"

  val serdeMap: Map[String, Object] = Map(
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> mockSRUrl,
    KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE -> classOf[Myrecord.MyRecord]
  )

  "must be able to serialize and deserialize specifc SRProto" in {
    val serializer: KafkaProtobufSerializer[Myrecord.MyRecord] = new KafkaProtobufSerializer()

    serializer.configure(serdeMap.asJava, false)

    val otherRecord: Other.OtherRecord = Other.OtherRecord.newBuilder().setOtherId(123).build()
    val record: Myrecord.MyRecord = Myrecord.MyRecord
      .newBuilder()
      .setF1("foo")
      .setF2(otherRecord)
      .build()

    val serializedRecord = serializer.serialize(dummyTopicName, record)

    val deserializer: KafkaProtobufDeserializer[Myrecord.MyRecord] = new KafkaProtobufDeserializer()
    deserializer.configure(serdeMap.asJava, false)

    val deserialized = deserializer.deserialize(dummyTopicName, serializedRecord)

    // companion.fromJavaProto(deserializer.deserialize(s, bytes))


    println(deserialized)
  }

}
