package com.example.fileingester.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.fileingester.model.LogEntry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, LogEntry> kafkaTemplate;
    private static final String TOPIC = "logs-topic";

    public void sendLog(LogEntry logEntry) {
        kafkaTemplate.send(TOPIC, logEntry);
    }
}
