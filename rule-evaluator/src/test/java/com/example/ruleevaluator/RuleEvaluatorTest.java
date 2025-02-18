package com.example.ruleevaluator;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.example.ruleevaluator.model.LogEntry;
import com.example.ruleevaluator.service.RuleEvaluator;
import com.example.ruleevaluator.service.RuleEvaluator.AlertData;

@SpringBootTest
@Transactional
@Rollback
public class RuleEvaluatorTest {

    private static final RuleEvaluator ruleEvaluator = new RuleEvaluator();

    @Test
    public void testErrorLogTriggersAlert() {
        LogEntry logEntry = new LogEntry(LocalDateTime.now(), "ComponentA", "[Thread] ERROR com.example.Class - Something went wrong");
        List<AlertData> alerts = ruleEvaluator.evaluate(logEntry);
        assertTrue(!alerts.isEmpty());
        
        AlertData firstAlert = alerts.get(0);

        assertEquals("ERROR", firstAlert.ruleTriggered);
        assertTrue(firstAlert.alertMessage.contains("Something went wrong"));
    }

    @Test
    public void testHighErrorRateTriggersAlert() {
        for (int i = 0; i < 10; i++) {
            LogEntry logEntry = new LogEntry(LocalDateTime.now(), "ComponentB", "[Thread] ERROR com.example.Class - Error " + i);
            ruleEvaluator.evaluate(logEntry);
        }

        LogEntry logEntry = new LogEntry(LocalDateTime.now(), "ComponentB", "[Thread] ERROR com.example.Class - Final Error");
        List<AlertData> alerts = ruleEvaluator.evaluate(logEntry);
        assertTrue(!alerts.isEmpty());

        AlertData lastAlert = alerts.get(alerts.size() - 1);

        assertEquals("HIGH ERROR RATE", lastAlert.ruleTriggered);
        assertTrue(lastAlert.alertMessage.contains("High error rate detected in ComponentB"));
        assertTrue(lastAlert.alertMessage.contains("Error 9"));
        assertTrue(lastAlert.alertMessage.contains("Final Error"));
    }

    @Test
    public void testHighLogActivityTriggersAlert() {
        for (int i = 0; i < 50; i++) {
            LogEntry logEntry = new LogEntry(LocalDateTime.now(), "ComponentC", "[Thread] INFO com.example.Class - Log " + i);
            ruleEvaluator.evaluate(logEntry);
        }

        LogEntry logEntry = new LogEntry(LocalDateTime.now(), "ComponentC", "[Thread] INFO com.example.Class - Last Log");
        List<AlertData> alerts = ruleEvaluator.evaluate(logEntry);
        assertTrue(!alerts.isEmpty());

        AlertData lastAlert = alerts.get(alerts.size() - 1);

        assertEquals("HIGH LOG ACTIVITY", lastAlert.ruleTriggered);
        assertTrue(lastAlert.alertMessage.contains("Very high logging activity detected in ComponentC"));
    }
}
