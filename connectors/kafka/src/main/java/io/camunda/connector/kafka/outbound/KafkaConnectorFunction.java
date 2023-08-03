/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.kafka.outbound;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule$;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.feel.ConnectorsObjectMapperSupplier;
import io.camunda.connector.kafka.outbound.model.KafkaConnectorRequest;
import io.camunda.connector.kafka.outbound.model.KafkaConnectorResponse;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.avro.Schema;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@OutboundConnector(
    name = "KAFKA",
    inputVariables = {"authentication", "topic", "message", "additionalProperties"},
    type = "io.camunda:connector-kafka:1")
public class KafkaConnectorFunction implements OutboundConnectorFunction {

  private final Function<Properties, Producer> producerCreatorFunction;

  private static final ObjectMapper objectMapper =
      ConnectorsObjectMapperSupplier.getCopy().enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

  public KafkaConnectorFunction() {
    this(KafkaProducer::new);
  }

  public KafkaConnectorFunction(final Function<Properties, Producer> producerCreatorFunction) {
    this.producerCreatorFunction = producerCreatorFunction;
  }

  @Override
  public Object execute(final OutboundConnectorContext context) {
    var connectorRequest = context.bindVariables(KafkaConnectorRequest.class);
    return executeConnector(connectorRequest);
  }

  public static byte[] produceAvroMessage(final KafkaConnectorRequest request) throws Exception {
    Schema raw = new Schema.Parser().setValidate(true).parse(request.getAvro().schema());
    AvroSchema schema = new AvroSchema(raw);
    AvroMapper avroMapper = new AvroMapper();
    return avroMapper.writer(schema).writeValueAsBytes(request.getMessage().getValue());
  }

  private KafkaConnectorResponse executeConnector(final KafkaConnectorRequest request) {
    Properties props = request.assembleKafkaClientProperties();
    Producer<String, Object> producer = producerCreatorFunction.apply(props);
    RecordMetadata recordMetadata;
    try {
      Object transformedValue = null;
      if (request.getAvro() != null) {
        transformedValue = produceAvroMessage(request);
      } else {
        transformedValue =
            request.getMessage().getValue() instanceof String
                ? (String) request.getMessage().getValue()
                : objectMapper.writeValueAsString(request.getMessage().getValue());
      }
      Future<RecordMetadata> kafkaResponse =
          producer.send(
              new ProducerRecord<>(
                  request.getTopic().getTopicName(),
                  String.valueOf(request.getMessage().getKey()),
                  transformedValue));
      KafkaConnectorResponse result = new KafkaConnectorResponse();
      recordMetadata = kafkaResponse.get(45, TimeUnit.SECONDS);
      result.setTopic(recordMetadata.topic());
      result.setPartition(recordMetadata.partition());
      result.setOffset(recordMetadata.offset());
      result.setTimestamp(recordMetadata.timestamp());
      return result;
    } catch (Exception e) {
      throw new ConnectorException("FAIL", "Kafka Producer execution exception", e);
    } finally {
      producer.close();
    }
  }
}
