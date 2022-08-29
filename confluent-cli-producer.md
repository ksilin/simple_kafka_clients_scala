# kafka-avro-console-producer

have not tried it yet

## with schema file(s)

* pipe file to property

`confluent kafka topic produce orders-avro --value-format avro --schema orders-avro-schema.json --parse-key --delimiter ":"`

## Questions

### what is the backing impl? 

IIRC, it was sarama for a while.

## check results

`./kafka-avro-console-consumer --bootstrap-server localhost:29092 --topic test --property schema.registry.url=http://localhost:8081 --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property print.key=true --property key.separator=":" --from-beginning`

### ref

`https://developer.confluent.io/tutorials/kafka-console-consumer-producer-avro/confluent.html`

