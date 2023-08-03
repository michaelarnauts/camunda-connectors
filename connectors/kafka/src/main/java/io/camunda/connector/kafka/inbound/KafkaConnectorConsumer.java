/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.kafka.inbound;

import static io.camunda.connector.kafka.inbound.KafkaPropertyTransformer.convertConsumerRecordToKafkaInboundMessage;
import static io.camunda.connector.kafka.inbound.KafkaPropertyTransformer.getKafkaProperties;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule$;
import io.camunda.connector.api.error.ConnectorInputException;
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.api.inbound.InboundConnectorResult;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetOutOfRangeException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConnectorConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaConnectorConsumer.class);

  private final InboundConnectorContext context;

  private ExecutorService executorService;

  public CompletableFuture<?> future;

  Consumer<String, Object> consumer;

  KafkaConnectorProperties elementProps;

  public static ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(DefaultScalaModule$.MODULE$)
          .registerModule(new JavaTimeModule())
          // deserialize unknown types as empty objects
          .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);

  private ObjectReader avroObjectReader;

  boolean shouldLoop = true;

  private final Function<Properties, Consumer<String, Object>> consumerCreatorFunction;

  public KafkaConnectorConsumer(
      final Function<Properties, Consumer<String, Object>> consumerCreatorFunction,
      final InboundConnectorContext connectorContext,
      final KafkaConnectorProperties elementProps) {
    this.consumerCreatorFunction = consumerCreatorFunction;
    this.context = connectorContext;
    this.elementProps = elementProps;
    this.executorService = Executors.newSingleThreadExecutor();
  }

  public void startConsumer() {
    this.future =
        CompletableFuture.runAsync(
            () -> {
              if (elementProps.getAvro() != null) {
                Schema schema =
                    new Schema.Parser().setValidate(true).parse(elementProps.getAvro().schema());
                AvroSchema avroSchema = new AvroSchema(schema);
                AvroMapper avroMapper = new AvroMapper();
                avroObjectReader = avroMapper.reader(avroSchema);
              }
              prepareConsumer();
              consume();
            },
            this.executorService);
  }

  private void prepareConsumer() {
    try {
      this.consumer = consumerCreatorFunction.apply(getKafkaProperties(elementProps, context));
      var partitions = assignTopicPartitions(consumer, elementProps.getTopic().getTopicName());
      Optional.ofNullable(elementProps.getOffsets())
          .ifPresent(offsets -> seekOffsets(consumer, partitions, offsets));
      reportUp();
    } catch (Exception ex) {
      LOG.error("Failed to initialize connector: {}", ex.getMessage());
      context.reportHealth(Health.down(ex));
      throw ex;
    }
  }

  private List<TopicPartition> assignTopicPartitions(
      Consumer<String, Object> consumer, String topic) {
    // dynamically assign partitions to be able to handle offsets
    List<PartitionInfo> partitions = consumer.partitionsFor(topic);
    List<TopicPartition> topicPartitions =
        partitions.stream()
            .map(partition -> new TopicPartition(partition.topic(), partition.partition()))
            .collect(Collectors.toList());
    consumer.assign(topicPartitions);
    return topicPartitions;
  }

  private void seekOffsets(
      Consumer<String, ?> consumer, List<TopicPartition> partitions, List<Long> offsets) {
    if (partitions.size() != offsets.size()) {
      throw new ConnectorInputException(
          new IllegalArgumentException(
              "Number of offsets provided is not equal the number of partitions!"));
    }
    for (int i = 0; i < offsets.size(); i++) {
      consumer.seek(partitions.get(i), offsets.get(i));
    }
    LOG.info("Kafka inbound connector initialized");
  }

  public void consume() {
    while (shouldLoop) {
      try {
        pollAndPublish();
        reportUp();
      } catch (Exception ex) {
        LOG.error("Failed to execute connector: {}", ex.getMessage());
        context.reportHealth(Health.down(ex));
        if (ex instanceof OffsetOutOfRangeException) {
          throw ex;
        }
      }
    }
    LOG.debug("Kafka inbound loop finished");
  }

  private void pollAndPublish() {
    LOG.debug("Polling the topics: {}", this.consumer.assignment());
    ConsumerRecords<String, Object> records = this.consumer.poll(Duration.ofMillis(500));
    for (ConsumerRecord<String, Object> record : records) {
      handleMessage(record);
    }
    if (!records.isEmpty()) {
      this.consumer.commitSync();
    }
  }

  private void handleMessage(ConsumerRecord<String, Object> record) {
    LOG.trace("Kafka message received: key = {}, value = {}", record.key(), record.value());
    var reader = avroObjectReader != null ? avroObjectReader : objectMapper.reader();
    var mappedMessage = convertConsumerRecordToKafkaInboundMessage(record, reader);
    InboundConnectorResult<?> result = this.context.correlate(mappedMessage);
    if (result.isActivated()) {
      LOG.debug("Inbound event correlated successfully: {}", result.getResponseData());
    } else {
      LOG.debug("Inbound event not correlated: {}", result.getErrorData());
    }
  }

  public void stopConsumer() throws ExecutionException, InterruptedException {
    this.shouldLoop = false;
    if (this.future != null && !this.future.isDone()) {
      this.future.get();
    }
    this.consumer.close();
    if (this.executorService != null) {
      this.executorService.shutdownNow();
    }
  }

  private void reportUp() {
    var details = new HashMap<String, Object>();
    details.put("group-id", consumer.groupMetadata().groupId());
    details.put("group-instance-id", consumer.groupMetadata().groupInstanceId().orElse("unknown"));
    details.put("group-generation-id", consumer.groupMetadata().generationId());
    context.reportHealth(Health.up(details));
  }
}
