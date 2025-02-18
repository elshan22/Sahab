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

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.fileingester.model.LogEntry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogReaderService {

    private final KafkaProducerService kafkaProducerService;

    private static final Pattern LOG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) (\\[(.*?)\\] (\\w+) ([\\w\\.]+) – (.+))"
    );

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    private static final String LOG_DIR = "logs";

    @Scheduled(fixedRate=10000)
    public void watchLogDirectory() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(LOG_DIR), "*.log")) {
            for (Path file : stream) {
                String componentName = extractComponentName(file.getFileName().toString());
                processLogFile(file, componentName);
                deleteLogFile(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processLogFile(Path file, String componentName) {
        try {
            Files.lines(file).forEach(line -> {
                LogEntry logEntry = parseLogLine(line, componentName);
                kafkaProducerService.sendLog(logEntry);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LogEntry parseLogLine(String line, String componentName) {
        Matcher matcher = LOG_PATTERN.matcher(line);

        LocalDateTime timestamp = LocalDateTime.now();
        String data = "";
        if (matcher.matches()) {
            timestamp = LocalDateTime.parse(matcher.group(1), formatter);
            data = matcher.group(2);
        }

        return new LogEntry(timestamp, componentName, data);
    }

    private String extractComponentName(String filename) {
        return filename.split("-")[0];
    }

    private void deleteLogFile(Path file) {
        try {
            Files.delete(file);
            System.out.println("Deleted file: " + file.getFileName());
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + file.getFileName());
            e.printStackTrace();
        }
    }

}
