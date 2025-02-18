package com.example.ruleevaluator.config;

import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import com.example.ruleevaluator.model.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KafkaDeserializer implements Deserializer {

    private ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void configure(Map configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }
@Override
    public Object deserialize(String s, byte[] data) {
        try {
            if (data == null){
                return null;
            }
            return objectMapper.readValue(new String(data, "UTF-8"), LogEntry.class);
        } catch (Exception e) {
            throw new SerializationException("Error when deserializing byte[] to Mediation Message.");
        }
    }
@Override
    public Object deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }
@Override
    public void close() {
        Deserializer.super.close();
    }
    
}
