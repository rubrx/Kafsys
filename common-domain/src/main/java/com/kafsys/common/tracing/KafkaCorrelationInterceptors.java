package com.kafsys.common.tracing;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka client interceptors that propagate {@link CorrelationId} across
 * asynchronous boundaries.
 *
 * <p><b>Producer</b>: attaches the current MDC correlation ID (or a fresh UUID
 * if none is set) as {@link CorrelationId#KAFKA_HEADER} on every outbound
 * record, so downstream consumers can pick it up.
 *
 * <p><b>Consumer</b>: for every polled record it does NOT set MDC directly
 * (Kafka's poll batches multiple records and MDC leakage across records would
 * be worse than nothing); instead, the Spring Kafka {@code RecordInterceptor}
 * is the correct integration point for the per-message MDC push/pop. Services
 * that use the listener-container factory pattern should wire the
 * {@link ConsumerMdcRecordInterceptor} bean provided per-service.
 *
 * <p>Wire the producer interceptor via:
 * {@code props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
 *     KafkaCorrelationInterceptors.Producer.class.getName());}
 */
public final class KafkaCorrelationInterceptors {

    private KafkaCorrelationInterceptors() {}

    /** Producer interceptor: writes MDC correlationId into record headers. */
    public static class Producer implements ProducerInterceptor<Object, Object> {
        @Override
        public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
            Header existing = record.headers().lastHeader(CorrelationId.KAFKA_HEADER);
            if (existing == null) {
                String id = MDC.get(CorrelationId.MDC_KEY);
                if (id == null || id.isBlank()) {
                    id = UUID.randomUUID().toString();
                }
                record.headers().add(CorrelationId.KAFKA_HEADER, id.getBytes(StandardCharsets.UTF_8));
            }
            return record;
        }

        @Override public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}
        @Override public void close() {}
        @Override public void configure(Map<String, ?> configs) {}
    }

    /**
     * Consumer interceptor placeholder for future integration (e.g. exporting
     * consumer lag with correlation labels). Kept as a stub so both
     * {@link ProducerConfig} and {@link ConsumerConfig} interceptor slots are
     * documented at the same call site.
     */
    public static class Consumer implements ConsumerInterceptor<Object, Object> {
        @Override
        public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
            return records;
        }
        @Override public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {}
        @Override public void close() {}
        @Override public void configure(Map<String, ?> configs) {}
    }

    /**
     * Pulls the correlation ID off a consumer record's headers and returns it
     * (or {@code null} if absent). Callers push/pop MDC around dispatch.
     */
    public static String readHeader(ConsumerRecord<?, ?> record) {
        Header h = record.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        if (h == null || h.value() == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
