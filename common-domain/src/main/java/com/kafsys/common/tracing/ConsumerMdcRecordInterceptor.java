package com.kafsys.common.tracing;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import java.util.UUID;

/**
 * Spring Kafka record interceptor that pushes the correlation ID from the
 * inbound record's headers into SLF4J MDC before the {@code @KafkaListener}
 * method runs, and removes it after the record is dispatched.
 *
 * <p>Wire it into each service's {@code ConcurrentKafkaListenerContainerFactory}:
 * <pre>{@code
 * factory.setRecordInterceptor(new ConsumerMdcRecordInterceptor<>());
 * }</pre>
 *
 * @param <K> record key type
 * @param <V> record value type
 */
public class ConsumerMdcRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

    @Override
    public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        String id = KafkaCorrelationInterceptors.readHeader(record);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(CorrelationId.MDC_KEY, id);
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
        MDC.remove(CorrelationId.MDC_KEY);
    }
}
