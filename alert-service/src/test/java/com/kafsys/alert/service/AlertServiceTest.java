package com.kafsys.alert.service;

import com.kafsys.alert.dto.AlertResponse;
import com.kafsys.alert.entity.Alert;
import com.kafsys.alert.repository.AlertRepository;
import com.kafsys.common.dto.PagedResponse;
import com.kafsys.common.enums.AlertType;
import com.kafsys.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock AlertRepository alerts;
    @InjectMocks AlertService service;

    private Alert alert(String id, boolean read) {
        Alert a = new Alert();
        a.setId(id);
        a.setAccountId("acct-1");
        a.setTransactionId("tx-1");
        a.setType(AlertType.TRANSACTION_COMPLETED);
        a.setMessage("done");
        a.setRead(read);
        return a;
    }

    @Test
    void markAsRead_flipsFlagAndStampsReadAt() {
        Alert a = alert("a-1", false);
        when(alerts.findById("a-1")).thenReturn(Optional.of(a));

        AlertResponse response = service.markAsRead("a-1");

        assertThat(response.read()).isTrue();
        assertThat(a.getReadAt()).isNotNull();
        verify(alerts).save(a);
    }

    @Test
    void markAsRead_missing_throws() {
        when(alerts.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markAsRead("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void countUnread_delegatesToRepository() {
        when(alerts.countByAccountIdAndRead("acct-1", false)).thenReturn(7L);
        assertThat(service.countUnread("acct-1")).isEqualTo(7L);
    }

    @Test
    void getAlertsByAccount_unreadOnly_usesFilteredQuery() {
        Page<Alert> page = new PageImpl<>(List.of(alert("a-1", false)));
        when(alerts.findByAccountIdAndRead(eq("acct-1"), eq(false), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<AlertResponse> result = service.getAlertsByAccount("acct-1", true, 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).read()).isFalse();
        verify(alerts, never()).findByAccountId(any(), any());
    }

    @Test
    void getAlertsByAccount_unreadFalse_returnsAll() {
        Page<Alert> page = new PageImpl<>(List.of(alert("a-1", true), alert("a-2", false)));
        when(alerts.findByAccountId(eq("acct-1"), any(Pageable.class))).thenReturn(page);

        PagedResponse<AlertResponse> result = service.getAlertsByAccount("acct-1", false, 0, 10);

        assertThat(result.content()).hasSize(2);
        verify(alerts, never()).findByAccountIdAndRead(any(), anyBoolean(), any());
    }

    @Test
    void getById_missing_throws() {
        when(alerts.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
