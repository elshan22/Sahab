package com.example.ruleevaluator.service;

import com.example.ruleevaluator.model.LogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RuleEvaluator {

    @Value("${timewindowminutes}")
    private int timeWindowMinutes;

    @Value("${errorThreshold}")
    private int errorThreshold;

    @Value("${highLogRateThreshold}")
    private int highLogRateThreshold;

    private final List<JsonNode> rules = new ArrayList<>();
    private final Map<String, Queue<LogEntry>> logHistory = new HashMap<>();

    public RuleEvaluator() {
        loadRulesFromFile("rules.json");
    }

    private void loadRulesFromFile(String filePath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(new File(filePath));
            rules.addAll(root.get("rules"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rules from file", e);
        }
    }

    public AlertData evaluate(LogEntry logEntry) {
        String componentName = logEntry.getComponentName();
        String logMessage = logEntry.getData();

        logHistory.putIfAbsent(componentName, new LinkedList<>());
        Queue<LogEntry> logs = logHistory.get(componentName);

        LocalDateTime now = LocalDateTime.now();
        cleanOldLogs(logs, now);
        logs.add(logEntry);

        // Check for ERROR or WARNING logs
        for (JsonNode rule : rules) {
            if (rule.has("type") && logMessage.contains(rule.get("type").asText())) {
                String alertMessage = rule.get("message").asText()
                        .replace("{component}", componentName) + logMessage;
                return new AlertData(rule.get("type").asText(), alertMessage);
            }
        }

        // Check for high error rate
        long errorCount = logs.stream()
                .filter(log -> log.getData().contains("ERROR"))
                .count();

        if (errorCount > errorThreshold) {
            JsonNode errorRule = findRuleByName("High Error Rate");
            if (errorRule != null) {
                String alertMessage = errorRule.get("message").asText()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(errorCount))
                        .replace("{timeWindow}", String.valueOf(timeWindowMinutes))
                        + getLastTwoLogs(logs);
                return new AlertData("HIGH ERROR RATE", alertMessage);
            }
        }

        // Check for high log activity
        if (logs.size() > highLogRateThreshold) {
            JsonNode logActivityRule = findRuleByName("High Log Activity");
            if (logActivityRule != null) {
                String alertMessage = logActivityRule.get("message").asText()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(logs.size()))
                        .replace("{time}", String.valueOf(timeWindowMinutes))
                        + getLastTwoLogs(logs);
                return new AlertData("HIGH LOG ACTIVITY", alertMessage);
            }
        }

        return null;
    }

    private void cleanOldLogs(Queue<LogEntry> logs, LocalDateTime now) {
        logs.removeIf(log -> log.getTimestamp().isBefore(now.minus(timeWindowMinutes, ChronoUnit.MINUTES)));
    }

    private JsonNode findRuleByName(String name) {
        return rules.stream().filter(rule -> rule.get("name").asText().equals(name)).findFirst().orElse(null);
    }

    private String getLastTwoLogs(Queue<LogEntry> logs) {
        return logs.stream().skip(Math.max(0, logs.size() - 2))
                .map(LogEntry::getData)
                .reduce("\nLast logs: ", (a, b) -> a + "\n" + b);
    }

    public static class AlertData {
        public String ruleTriggered;
        public String alertMessage;

        public AlertData(String ruleTriggered, String alertMessage) {
            this.ruleTriggered = ruleTriggered;
            this.alertMessage = alertMessage;
        }
    }
}
