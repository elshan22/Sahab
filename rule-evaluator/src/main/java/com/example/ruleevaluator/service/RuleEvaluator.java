package com.example.ruleevaluator.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.ruleevaluator.model.LogEntry;
import com.example.ruleevaluator.model.Rule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RuleEvaluator {

    private static final int TIME_WINDOW_MINUTES = 5;
    private static final int ERROR_THRESHOLD = 5;
    private static final int HIGH_LOG_RATE = 20;

    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Queue<LogEntry>> logHistory = new HashMap<>();

    private static final Pattern LOG_PATTERN = Pattern.compile("\\[.*\\]\\s*(\\S+).*-\\s*(.*)");

    public RuleEvaluator() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("rules.json");
            ObjectMapper objectMapper = new ObjectMapper();
            List<Rule> loadedRules = objectMapper.readValue(inputStream, new TypeReference<List<Rule>>() {});
            rules.addAll(loadedRules);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rules.json", e);
        }
    }

    public List<AlertData> evaluate(LogEntry logEntry) {
        String componentName = logEntry.getComponentName();
        String[] logData = getLogTypeAndMessage(logEntry);

        logHistory.putIfAbsent(componentName, new LinkedList<>());
        Queue<LogEntry> logs = logHistory.get(componentName);

        LocalDateTime now = LocalDateTime.now();
        cleanOldLogs(logs, now);
        logs.add(logEntry);

        List<AlertData> alerts = new ArrayList<>();

        for (Rule rule : rules) {
            if (rule.getType() != null && logData[0].equals(rule.getType())) {
                String alertMessage = rule.getMessage()
                        .replace("{component}", componentName) + logData[1];
                alerts.add(new AlertData(rule.getType(), alertMessage));
            }
        }

        List<LogEntry> errors = logs.stream()
                .filter(log -> getLogTypeAndMessage(log)[0].equals("ERROR")).toList();

        if (errors.size() > ERROR_THRESHOLD) {
            Rule errorRule = findRuleByName("High Error Rate");
            System.out.println(errorRule.getName());
            if (errorRule != null) {
                String alertMessage = errorRule.getMessage()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(errors.size()))
                        .replace("{timeWindow}", String.valueOf(TIME_WINDOW_MINUTES))
                        + getLastTwoLogs(errors);
                alerts.add(new AlertData("HIGH ERROR RATE", alertMessage));
            }
        }

        List<LogEntry> warnings = logs.stream()
                .filter(log -> getLogTypeAndMessage(log)[0].equals("WARNING")).toList();

        if (warnings.size() > ERROR_THRESHOLD) {
            Rule errorRule = findRuleByName("High Warning Rate");
            if (errorRule != null) {
                String alertMessage = errorRule.getMessage()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(warnings.size()))
                        .replace("{timeWindow}", String.valueOf(TIME_WINDOW_MINUTES))
                        + getLastTwoLogs(warnings);
                alerts.add(new AlertData("HIGH WARNING RATE", alertMessage));
            }
        }

        if (logs.size() > HIGH_LOG_RATE) {
            Rule logActivityRule = findRuleByName("High Log Activity");
            if (logActivityRule != null) {
                String alertMessage = logActivityRule.getMessage()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(logs.size()))
                        .replace("{timeWindow}", String.valueOf(TIME_WINDOW_MINUTES));
                alerts.add(new AlertData("HIGH LOG ACTIVITY", alertMessage));
            }
        }

        return alerts;
    }

    private void cleanOldLogs(Queue<LogEntry> logs, LocalDateTime now) {
        logs.removeIf(log -> log.getTimestamp().isBefore(now.minus(TIME_WINDOW_MINUTES, ChronoUnit.MINUTES)));
    }

    private Rule findRuleByName(String name) {
        return rules.stream().filter(rule -> rule.getName().equals(name)).findFirst().orElse(null);
    }

    private String getLastTwoLogs(List<LogEntry> logs) {
        return "\n" + getLogTypeAndMessage(logs.get(logs.size() - 2))[1]
              +"\n" + getLogTypeAndMessage(logs.get(logs.size() - 1))[1];
    }

    private String[] getLogTypeAndMessage(LogEntry logEntry) {
        String logMessage = logEntry.getData();
        String logType = logEntry.getData();

        Matcher matcher = LOG_PATTERN.matcher(logEntry.getData());

        if (matcher.find()) {
            logType = matcher.group(1);
            logMessage = matcher.group(2);
        }

        return new String[]{logType, logMessage};
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
