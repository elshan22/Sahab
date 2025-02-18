package com.example.ruleevaluator.service;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.example.ruleevaluator.model.Alert;
import com.example.ruleevaluator.model.LogEntry;
import com.example.ruleevaluator.repository.AlertRepository;
import com.example.ruleevaluator.service.RuleEvaluator.AlertData;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private RuleEvaluator ruleEvaluator;

    @KafkaListener(topics = "logs-topic", groupId = "log-consumer-group", containerFactory = "concurrentKafkaListenerContainerFactory")
    public void consumeLog(ConsumerRecord<String, LogEntry> consumerRecord, Acknowledgment acknowledgment) {
        List<RuleEvaluator.AlertData> alerts = ruleEvaluator.evaluate(consumerRecord.value());

        for (AlertData alertData: alerts) {
            Alert alert = new Alert(
                null,
                consumerRecord.value().getTimestamp(),
                consumerRecord.value().getComponentName(),
                alertData.ruleTriggered,
                alertData.alertMessage
            );
            alertRepository.save(alert);
        }

        acknowledgment.acknowledge();
    }
}
