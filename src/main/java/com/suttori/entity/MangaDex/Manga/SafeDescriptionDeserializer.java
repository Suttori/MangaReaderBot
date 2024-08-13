package com.suttori.entity.MangaDex.Manga;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SafeDescriptionDeserializer extends JsonDeserializer<Map<String, String>> {
    @Override
    public Map<String, String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node.isObject()) {
            Map<String, String> result = new HashMap<>();
            node.fields().forEachRemaining(entry -> result.put(entry.getKey(), entry.getValue().asText()));
            return result;
        } else if (node.isArray()) {
            return new HashMap<>(); // Возвращаем пустую мапу, если поле оказалось массивом
        } else {
            return null; // Или другой способ обработки невалидных данных
        }
    }
}
