package com.kafsys.alert.service;

import com.kafsys.alert.entity.Alert;
import com.kafsys.alert.repository.AlertRepository;
import com.kafsys.common.event.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertEventConsumer.class);

    private final AlertRepository alertRepository;

    public AlertEventConsumer(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @KafkaListener(topics = "alerts", groupId = "alert-persister",
                   containerFactory = "alertEventListenerFactory")
    @Transactional
    public void handleAlert(AlertEvent event) {
        Alert alert = new Alert();
        alert.setId(event.getAlertId());
        alert.setAccountId(event.getAccountId());
        alert.setTransactionId(event.getTransactionId());
        alert.setType(event.getType());
        alert.setMessage(event.getMessage());
        alert.setTriggeredAt(event.getTriggeredAt());
        alert.setRead(false);

        alertRepository.save(alert);
        log.info("Alert persisted: id={} type={} account={}", alert.getId(), alert.getType(), alert.getAccountId());
    }
}
