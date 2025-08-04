package com.practice.io;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.TypeAdapter;

import java.io.IOException;
import java.time.Instant;

/**
 * Tiny wrapper around Gson that:
 *  • serialises nulls
 *  • ignores unknown fields when deserialising
 *  • pretty-prints for deterministic tests
 *  • handles Java time types (Instant)
 */
public final class JsonSerializer {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .create();

    private JsonSerializer() { }

    /** Serialises any POJO (records included) to JSON UTF-8 String. */
    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    /** Deserialises JSON – unknown fields are ignored. */
    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }

    /**
     * Custom TypeAdapter for java.time.Instant to avoid Java 17 module system issues.
     * Serializes as ISO-8601 string and deserializes back to Instant.
     */
    private static class InstantTypeAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return Instant.parse(in.nextString());
        }
    }
}