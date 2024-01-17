package io.github.wsxyeah.gsonksp.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DeserializationTest {

    private static class UserGsonAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() == User.class) {
                return (TypeAdapter<T>) new UserGsonAdapter(gson);
            }
            return null;
        }
    }

    @Test
    void testDeserialization() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new UserGsonAdapterFactory())
                .create();
        @Language("JSON") String json = "{\n" +
                "  \"some_string\": \"12345\",\n" +
                "  \"some_int\": 5555555,\n" +
                "  \"some_long\": 99999999999999,\n" +
                "  \"some_short\": 3333,\n" +
                "  \"some_byte\": 10,\n" +
                "  \"some_float\": 3.14,\n" +
                "  \"some_double\": 3.141592653589793,\n" +
                "  \"some_boolean\": true,\n" +
                "  \"some_integer_list\": [\n" +
                "    1,\n" +
                "    2,\n" +
                "    3\n" +
                "  ],\n" +
                "  \"some_map\": {\n" +
                "    \"key1\": \"value1\",\n" +
                "    \"key2\": \"value2\"\n" +
                "  },\n" +
                "  \"nested_map\": {\n" +
                "    \"key1\": {\n" +
                "      \"key11\": \"value1\",\n" +
                "      \"key12\": \"value2\"\n" +
                "    },\n" +
                "    \"key2\": {\n" +
                "      \"key21\": \"value1\",\n" +
                "      \"key22\": \"value2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        User user = gson.fromJson(json, User.class);
        assertNotNull(user);
        assertEquals("12345", user.someString);
        assertEquals(5555555, user.someInt);
        assertEquals(99999999999999L, user.someLong);
        assertEquals(3333, user.someShort);
        assertEquals(10, user.someByte);
        assertEquals(3.14f, user.someFloat, 0.001f);
        assertEquals(3.141592653589793, user.someDouble, 0.000000000000001);
        assertEquals(true, user.someBoolean);
        assertEquals(3, user.someIntegerList.size());
        assertEquals(Arrays.asList(1, 2, 3), user.someIntegerList);
        assertEquals(2, user.someMap.size());
        assertEquals(Map.of("key1", "value1", "key2", "value2"), user.someMap);
        assertEquals(2, user.nestedMap.size());
        assertEquals(Map.of(
                "key1", Map.of("key11", "value1", "key12", "value2"),
                "key2", Map.of("key21", "value1", "key22", "value2")
        ), user.nestedMap);
    }
}
