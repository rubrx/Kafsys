package com.kafsys.transaction;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.kafsys.common.enums.TransactionStatus;
import com.kafsys.common.event.TransactionEvent;
import com.kafsys.transaction.service.TransactionSagaService;

import java.time.Duration;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableDiscoveryClient
@EnableKafkaStreams
@EnableAsync
public class TransactionServiceApp {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceApp.class);

    @Autowired
    private TransactionSagaService sagaService;

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApp.class, args);
    }

    // ── Kafka Topic Definitions ─────────────────────────────────────────────

    @Bean
    public NewTopic transactionsTopic() {
        return TopicBuilder.name("transactions")
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic accountTransactionsTopic() {
        return TopicBuilder.name("account-transactions")
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic paymentTransactionsTopic() {
        return TopicBuilder.name("payment-transactions")
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic alertsTopic() {
        return TopicBuilder.name("alerts")
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ── Kafka Streams SAGA Topology ─────────────────────────────────────────
    //
    // account-transactions (ACCOUNT_VALIDATED/REJECTED)
    //   ↓
    //   join(payment-transactions, 30s window)
    //   ↓
    // sagaService.resolveSaga(accountEvent, paymentEvent) → COMPLETED/ROLLED_BACK
    //   ↓
    // transactions (final status)

    @Bean
    public KStream<String, TransactionEvent> transactionSagaStream(StreamsBuilder builder) {
        JsonSerde<TransactionEvent> eventSerde = new JsonSerde<>(TransactionEvent.class);

        KStream<String, TransactionEvent> accountStream = builder
                .stream("account-transactions", Consumed.with(Serdes.String(), eventSerde));

        KStream<String, TransactionEvent> paymentStream = builder
                .stream("payment-transactions", Consumed.with(Serdes.String(), eventSerde));

        KStream<String, TransactionEvent> resolvedStream = accountStream.join(
                paymentStream,
                sagaService::resolveSaga,
                JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(30)),
                StreamJoined.with(Serdes.String(), eventSerde, eventSerde)
        );

        resolvedStream
                .peek((key, event) -> log.info("SAGA resolved: txId={} status={}", key, event.getStatus()))
                .to("transactions", Produced.with(Serdes.String(), eventSerde));

        return resolvedStream;
    }

    @Bean
    public KTable<String, TransactionEvent> transactionStateStore(StreamsBuilder builder) {
        KeyValueBytesStoreSupplier store = Stores.persistentKeyValueStore("transaction-store");
        JsonSerde<TransactionEvent> eventSerde = new JsonSerde<>(TransactionEvent.class);

        return builder.stream("transactions", Consumed.with(Serdes.String(), eventSerde))
                .toTable(Materialized.<String, TransactionEvent>as(store)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(eventSerde));
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("kafsys-tx-");
        executor.initialize();
        return executor;
    }
}
