package com.example.ruleevaluator.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogEntry {
    private LocalDateTime timestamp;
    private String componentName;
    private String data;
}
