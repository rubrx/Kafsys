package com.kafsys.alert.service;

import com.kafsys.alert.entity.Alert;
import com.kafsys.alert.repository.AlertRepository;
import com.kafsys.common.enums.AlertType;
import com.kafsys.common.event.AlertEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertEventConsumerTest {

    @Mock AlertRepository alerts;
    @InjectMocks AlertEventConsumer consumer;

    @Test
    void handleAlert_mapsEventOntoEntity_andSaves() {
        LocalDateTime triggered = LocalDateTime.now();
        AlertEvent event = new AlertEvent(
                "alert-1", "acct-9", "tx-42", AlertType.LARGE_TRANSACTION, "over threshold");
        event.setTriggeredAt(triggered);

        consumer.handleAlert(event);

        ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(saved.capture());
        Alert alert = saved.getValue();
        assertThat(alert.getId()).isEqualTo("alert-1");
        assertThat(alert.getAccountId()).isEqualTo("acct-9");
        assertThat(alert.getTransactionId()).isEqualTo("tx-42");
        assertThat(alert.getType()).isEqualTo(AlertType.LARGE_TRANSACTION);
        assertThat(alert.getMessage()).isEqualTo("over threshold");
        assertThat(alert.getTriggeredAt()).isEqualTo(triggered);
        assertThat(alert.isRead()).isFalse();
    }
}
