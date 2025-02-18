package com.example.apiservice.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.apiservice.model.Alert;
import com.example.apiservice.service.AlertService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<Alert> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/{componentName}")
    public List<Alert> getAlertsByComponent(@PathVariable String componentName) {
        return alertService.getAlertsByComponent(componentName);
    }

    @GetMapping("/time-range")
    public List<Alert> getAlertsInTimeRange(
            @RequestParam("start") String start, 
            @RequestParam("end") String end) {
        LocalDateTime startTime = LocalDateTime.parse(start);
        LocalDateTime endTime = LocalDateTime.parse(end);
        return alertService.getAlertsInTimePeriod(startTime, endTime);
    }
}
