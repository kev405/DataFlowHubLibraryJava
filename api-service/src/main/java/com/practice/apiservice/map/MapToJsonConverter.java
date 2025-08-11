package com.practice.apiservice.map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter(autoApply = false)
public class MapToJsonConverter implements AttributeConverter<Map<String,String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override public String convertToDatabaseColumn(Map<String, String> attribute) {
        try { return attribute == null ? "{}" : MAPPER.writeValueAsString(attribute); }
        catch (Exception e) { throw new IllegalArgumentException("Could not serialize map to JSON", e); }
    }
    @Override public Map<String, String> convertToEntityAttribute(String dbData) {
        try { return dbData == null || dbData.isBlank()
                ? Map.of() : MAPPER.readValue(dbData, new TypeReference<>() {}); }
        catch (Exception e) { throw new IllegalArgumentException("Could not deserialize JSON to map", e); }
    }
}