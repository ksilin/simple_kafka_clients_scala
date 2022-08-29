# kafka-avro-console-producer

needs connection to SR. if not provided explicitly, assumes localhost:8081

## with schema file(s)

* pipe file to property
* 
`--property value.schema="$(< /etc/tutorial/orders-avro-schema.json)"`

## with literal schema(s)

```shell
docker exec -it schema-registry-us kafka-avro-console-producer 
--broker-list broker-us:9092 
--topic sourceclustertopic \
--property value.schema='{
    "type": "record",
    "name": "product",
    "fields": [
      { "name": "customer_id", "type": "int", "doc": "Customer ID" },
      { "name": "firstname", "type": "string", "doc": "Customer firstname"},
      { "name": "lastname", "type": "string", "doc": "Customer lastname" }
    ]
}'
--property key.serializer=org.apache.kafka.common.serialization.StringSerializer \
--property parse.key=true \
--property key.separator=":" \
--property schema.registry.url=http://localhost:8081
1:{"customer_id": 4, "firstname": "Herbert", "lastname": "Cain", "age": 44}
2:{"customer_id": 5, "firstname": "Hadley", "lastname": "Santiago", "age": 55}
3:{"customer_id": 6, "firstname": "Stuart", "lastname": "Markham", "age": 66}
```

## with SR-provided schema 

## to ccloud with SR

```shell
kafka-avro-console-producer 
--broker-list https://pkc-22kdq.eu-west-1.aws.confluent.cloud:9092 
--topic sr_cse_day 
--producer.config ccloud.aws.cl_demo1.lkc-7mn0p.110947.props 
--property "schema.registry.url=https://psrc-lo5k9.eu-central-1.aws.confluent.cloud" 
--property value.schema='{"type":"record","name":"myrecord","fields":[{"name":"name","type":"string"}]}' 
--property schema.registry.basic.auth.user.info=KEY:SECRET  
--property basic.auth.credentials.source=USER_INFO
```

## with data file

```shell
> cat data.file
1:{"name":"alice"}
2:{"name":"bob"}
3:{"name":"cole"}
4:{"name":"dale"}
```

```shell
cat data.file | ./kafka-avro-console-producer ...
```

## timestamp? 

you cannot add timestamp metadata to your records. 

what you could do is to produce a timestamp within your messages and then use ksql to use that timestamp

## key schema

`--property key.schema='{"type":"int"}'`

## SR with SSL

```shell
--property schema.registry.ssl.truststore.location=/etc/kafka/security/schema.registry.client.truststore.jks 
--property schema.registry.ssl.truststore.password=myTrustorePassword 
--property schema.registry.ssl.keystore.location=/etc/kafka/security/schema.registry.client.keystore.jks 
--property schema.registry.ssl.keystore.password=myKeystorePassword
```

## Questions

### is the schema stored in SR? 

yes, look at 

```shell
> http :8081/subjects/test-value/versions/1
HTTP/1.1 200 OK
Content-Encoding: gzip
Content-Length: 122
Content-Type: application/vnd.schemaregistry.v1+json
Date: Sat, 27 Aug 2022 08:46:39 GMT
Vary: Accept-Encoding, User-Agent

{
    "id": 1,
    "schema": "{\"type\":\"record\",\"name\":\"s1\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}",
    "subject": "test-value",
    "version": 1
}
```

## check results

`./kafka-avro-console-consumer --bootstrap-server localhost:29092 --topic test --property schema.registry.url=http://localhost:8081 --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property print.key=true --property key.separator=":" --from-beginning`

### ref

https://developer.confluent.io/tutorials/kafka-console-consumer-producer-avro/kafka.html
