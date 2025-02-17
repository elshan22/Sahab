package com.example.fileingester.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.fileingester.model.LogEntry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogReaderService {

    private final KafkaProducerService kafkaProducerService;

    private static final long FIXED_RATE = 10000;

    private static final Pattern LOG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) \\[(.*?)\\] (\\w+) ([\\w\\.]+) – (.+)"
    );

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");


    @Value("${log.directory}")
    private static String LOG_DIR;

    @Scheduled(fixedRate = FIXED_RATE)
    public void watchLogDirectory() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(LOG_DIR), "*.log")) {
            for (Path file : stream) {
                processLogFile(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processLogFile(Path file) {
        try {
            Files.lines(file).forEach(line -> {
                LogEntry logEntry = parseLogLine(line);
                kafkaProducerService.sendLog(logEntry);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static LogEntry parseLogLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);

        if (matcher.matches()) {
            LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), formatter);
            String threadName = matcher.group(2);
            String logLevel = matcher.group(3);
            String className = matcher.group(4);
            String message = matcher.group(5);

            return new LogEntry(
                timestamp,
                "MyComponent",
                logLevel,
                threadName,
                className,
                message
            );
        }

        return new LogEntry(LocalDateTime.now(), "Unknown", "INFO", "Unknown", "Unknown", line);
    }
}
