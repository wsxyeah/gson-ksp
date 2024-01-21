package io.github.wsxyeah.gsonksp.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DeserializationTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new TestUserGsonAdapterFactory())
                .create();
    }

    @Test
    void testNullObjectDeserialization() {
        User user = gson.fromJson("null", User.class);
        assertNull(user);
    }

    @Test
    void testWrongTypeDeserialization() {
        JsonSyntaxException ex = assertThrows(JsonSyntaxException.class, () ->
                gson.fromJson("123", User.class)
        );
        assertTrue(ex.getMessage().contains("Expected BEGIN_OBJECT but was NUMBER"));
    }

    @Test
    void testNullFieldsDeserialization() {
        @Language("JSON") String json = "{\n" +
                "  \"some_string\": null,\n" +
                "  \"some_int\": null,\n" +
                "  \"some_long\": null,\n" +
                "  \"some_short\": null,\n" +
                "  \"some_byte\": null,\n" +
                "  \"some_float\": null,\n" +
                "  \"some_double\": null,\n" +
                "  \"some_boolean\": null,\n" +
                "  \"some_integer_list\": null,\n" +
                "  \"some_map\": null,\n" +
                "  \"nested_map\": null\n" +
                "}";
        User user = gson.fromJson(json, User.class);
        assertNotNull(user);
        assertNull(user.someString);
        assertEquals(0, user.someInt);
        assertEquals(0, user.someLong);
        assertEquals(0, user.someShort);
        assertEquals(0, user.someByte);
        assertEquals(0, user.someFloat);
        assertEquals(0, user.someDouble);
        assertEquals(false, user.someBoolean);
        assertNull(user.someIntegerList);
        assertNull(user.someMap);
        assertNull(user.nestedMap);
    }

    @Test
    void testDeserialization() {
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
