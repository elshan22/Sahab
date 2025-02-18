package com.example.ruleevaluator.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.ruleevaluator.model.LogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final Pattern LOG_PATTERN = Pattern.compile("\\[.*\\]\\s*(\\S+).*–\\s*(.*)");

    public RuleEvaluator() {
        try {
            ClassPathResource resource = new ClassPathResource("rules.json");
            InputStream inputStream = resource.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            rules.addAll((Collection<? extends JsonNode>) objectMapper.readTree(inputStream));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rules.json", e);
        }
    }

    public AlertData evaluate(LogEntry logEntry) {
        String componentName = logEntry.getComponentName();
        String[] logData = getLogTypeAndMessage(logEntry);

        logHistory.putIfAbsent(componentName, new LinkedList<>());
        Queue<LogEntry> logs = logHistory.get(componentName);

        LocalDateTime now = LocalDateTime.now();
        cleanOldLogs(logs, now);
        logs.add(logEntry);

        for (JsonNode rule : rules) {
            if (rule.has("type") && logData[0].equals(rule.get("type").asText())) {
                String alertMessage = rule.get("message").asText()
                        .replace("{component}", componentName) + logData[1];
                return new AlertData(rule.get("type").asText(), alertMessage);
            }
        }

        List<LogEntry> errors = logs.stream()
                .filter(log -> getLogTypeAndMessage(log)[0].equals("ERROR")).toList();

        if (errors.size() > errorThreshold) {
            JsonNode errorRule = findRuleByName("High Error Rate");
            if (errorRule != null) {
                String alertMessage = errorRule.get("message").asText()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(errors.size()))
                        .replace("{timeWindow}", String.valueOf(timeWindowMinutes))
                        + getLastTwoLogs(errors);
                return new AlertData("HIGH ERROR RATE", alertMessage);
            }
        }

        List<LogEntry> warnings = logs.stream()
                .filter(log -> getLogTypeAndMessage(log)[0].equals("WARNING")).toList();

        if (warnings.size() > errorThreshold) {
            JsonNode errorRule = findRuleByName("High Warning Rate");
            if (errorRule != null) {
                String alertMessage = errorRule.get("message").asText()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(warnings.size()))
                        .replace("{timeWindow}", String.valueOf(timeWindowMinutes))
                        + getLastTwoLogs(warnings);
                return new AlertData("HIGH WARNING RATE", alertMessage);
            }
        }

        if (logs.size() > highLogRateThreshold) {
            JsonNode logActivityRule = findRuleByName("High Log Activity");
            if (logActivityRule != null) {
                String alertMessage = logActivityRule.get("message").asText()
                        .replace("{component}", componentName)
                        .replace("{amount}", String.valueOf(logs.size()))
                        .replace("{timeWindow}", String.valueOf(timeWindowMinutes));
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
