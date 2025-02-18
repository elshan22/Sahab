package com.example.apiservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.apiservice.model.Alert;
import com.example.apiservice.repository.AlertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public List<Alert> getAllAlerts() {
        return alertRepository.findAllByOrderByTimestampAsc();
    }

    public List<Alert> getAlertsByComponent(String componentName) {
        return alertRepository.findByComponentNameOrderByTimestampAsc(componentName);
    }

    public List<Alert> getAlertsInTimePeriod(LocalDateTime start, LocalDateTime end) {
        return alertRepository.findByTimestampBetweenOrderByTimestampAsc(start, end);
    }
}
