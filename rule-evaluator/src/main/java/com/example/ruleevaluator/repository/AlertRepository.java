package com.example.ruleevaluator.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ruleevaluator.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {
}
