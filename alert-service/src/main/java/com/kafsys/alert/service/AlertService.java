package com.kafsys.alert.service;

import com.kafsys.alert.dto.AlertResponse;
import com.kafsys.alert.entity.Alert;
import com.kafsys.alert.repository.AlertRepository;
import com.kafsys.common.dto.PagedResponse;
import com.kafsys.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AlertResponse> getAlertsByAccount(String accountId, Boolean unreadOnly,
                                                            int page, int size) {
        Page<Alert> result = (unreadOnly != null && unreadOnly)
                ? alertRepository.findByAccountIdAndRead(accountId, false, PageRequest.of(page, size))
                : alertRepository.findByAccountId(accountId, PageRequest.of(page, size));

        return PagedResponse.of(
                result.getContent().stream().map(AlertResponse::from).toList(),
                page, size, result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AlertResponse> getAllAlerts(int page, int size) {
        Page<Alert> result = alertRepository.findAll(PageRequest.of(page, size));
        return PagedResponse.of(
                result.getContent().stream().map(AlertResponse::from).toList(),
                page, size, result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AlertResponse getById(String id) {
        return alertRepository.findById(id)
                .map(AlertResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }

    @Transactional
    public AlertResponse markAsRead(String id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        alert.setRead(true);
        alert.setReadAt(LocalDateTime.now());
        alertRepository.save(alert);
        return AlertResponse.from(alert);
    }

    @Transactional(readOnly = true)
    public long countUnread(String accountId) {
        return alertRepository.countByAccountIdAndRead(accountId, false);
    }
}
