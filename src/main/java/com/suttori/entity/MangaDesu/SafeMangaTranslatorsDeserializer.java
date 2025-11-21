package com.suttori.entity.MangaDesu;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SafeMangaTranslatorsDeserializer extends JsonDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        List<String> result = new ArrayList<>();

        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> result.add(entry.getValue().asText()));
        } else if (node.isArray()) {
            node.forEach(element -> result.add(element.asText()));
        }

        return result;
    }
}
