package com.example.ruleevaluator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.ruleevaluator.model.Alert;
import com.example.ruleevaluator.model.LogEntry;
import com.example.ruleevaluator.repository.AlertRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    @Autowired
    private final AlertRepository alertRepository;
    @Autowired
    private final RuleEvaluator ruleEvaluator;

    @KafkaListener(topics = "logs-topic", groupId = "log-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeLog(LogEntry logEntry) {
        RuleEvaluator.AlertData alertData = ruleEvaluator.evaluate(logEntry);

        if (alertData != null) {
            Alert alert = new Alert(
                null, 
                logEntry.getTimestamp(), 
                logEntry.getComponentName(), 
                alertData.ruleTriggered, 
                alertData.alertMessage
            );
            alertRepository.save(alert);
        }
    }
}
