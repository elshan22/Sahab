package com.example.apiservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.apiservice.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByComponentNameOrderByTimestampAsc(String componentName);
    List<Alert> findByTimestampBetweenOrderByTimestampAsc(LocalDateTime start, LocalDateTime end);
    List<Alert> findAllByOrderByTimestampAsc();
}
