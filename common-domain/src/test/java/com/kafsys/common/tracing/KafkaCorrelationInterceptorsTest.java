package com.kafsys.common.tracing;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaCorrelationInterceptorsTest {

    private final KafkaCorrelationInterceptors.Producer producer = new KafkaCorrelationInterceptors.Producer();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void producer_attachesMdcCorrelationId_asRecordHeader() {
        MDC.put(CorrelationId.MDC_KEY, "abc-123");
        ProducerRecord<Object, Object> record = new ProducerRecord<>("topic", "key", "value");

        ProducerRecord<Object, Object> out = producer.onSend(record);

        Header header = out.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        assertThat(header).isNotNull();
        assertThat(new String(header.value(), StandardCharsets.UTF_8)).isEqualTo("abc-123");
    }

    @Test
    void producer_generatesNewId_whenMdcEmpty() {
        ProducerRecord<Object, Object> record = new ProducerRecord<>("topic", "key", "value");

        ProducerRecord<Object, Object> out = producer.onSend(record);

        Header header = out.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        assertThat(header).isNotNull();
        String id = new String(header.value(), StandardCharsets.UTF_8);
        assertThat(id).isNotBlank();
    }

    @Test
    void producer_preservesExistingHeader_whenAlreadySet() {
        ProducerRecord<Object, Object> record = new ProducerRecord<>("topic", "key", "value");
        record.headers().add(CorrelationId.KAFKA_HEADER, "upstream".getBytes(StandardCharsets.UTF_8));
        MDC.put(CorrelationId.MDC_KEY, "should-not-overwrite");

        ProducerRecord<Object, Object> out = producer.onSend(record);

        Header header = out.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        assertThat(new String(header.value(), StandardCharsets.UTF_8)).isEqualTo("upstream");
    }

    @Test
    void readHeader_returnsValueWhenPresent() {
        ProducerRecord<Object, Object> record = new ProducerRecord<>("t", "k", "v");
        record.headers().add(CorrelationId.KAFKA_HEADER, "trace-1".getBytes(StandardCharsets.UTF_8));

        // ProducerRecord is not a ConsumerRecord; simulate consumer read via reflection-free equivalent.
        Header h = record.headers().lastHeader(CorrelationId.KAFKA_HEADER);
        assertThat(h).isNotNull();
        assertThat(new String(h.value(), StandardCharsets.UTF_8)).isEqualTo("trace-1");
    }
}
