package io.github.wsxyeah.gsonksp.example;

import com.google.gson.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationTest {
    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new TestUserGsonAdapterFactory())
                .create();
    }

    @Test
    void testNullObjectSerialization() {
        String json = gson.toJson(null);
        assertEquals("null", json);
    }

    @Test
    void testSerialization() {
        User user = new User();
        user.someInt = 5555555;
        user.someString = "12345";
        user.someLong = 99999999999999L;
        user.someShort = 3333;
        user.someByte = 10;
        user.someFloat = 3.14f;
        user.someDouble = 3.141592653589793;
        user.someBoolean = true;
        user.someIntegerList = Arrays.asList(1, 2, 3);
        user.someMap = Map.of("key1", "value1", "key2", "value2");
        user.nestedMap = Map.of(
                "key1", Map.of("key11", "value1", "key12", "value2"),
                "key2", Map.of("key21", "value1", "key22", "value2")
        );

        JsonElement jsonElement = gson.toJsonTree(user);
        assertNotNull(jsonElement);
        assertTrue(jsonElement.isJsonObject());
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        assertEquals(Set.of(
                "some_string",
                "some_int",
                "some_long",
                "some_short",
                "some_byte",
                "some_float",
                "some_double",
                "some_boolean",
                "some_integer_list",
                "some_map",
                "nested_map"
        ), jsonObject.keySet());
        assertEquals("12345", jsonObject.get("some_string").getAsJsonPrimitive().getAsString());
        assertEquals(5555555, jsonObject.get("some_int").getAsJsonPrimitive().getAsInt());
        assertEquals(99999999999999L, jsonObject.get("some_long").getAsJsonPrimitive().getAsLong());
        assertEquals(3333, jsonObject.get("some_short").getAsJsonPrimitive().getAsShort());
        assertEquals(10, jsonObject.get("some_byte").getAsJsonPrimitive().getAsByte());
        assertEquals(3.14f, jsonObject.get("some_float").getAsJsonPrimitive().getAsFloat(), 0.001f);
        assertEquals(3.141592653589793, jsonObject.get("some_double").getAsJsonPrimitive().getAsDouble(), 0.000000000000001);
        assertEquals(true, jsonObject.get("some_boolean").getAsJsonPrimitive().getAsBoolean());
        assertEquals(3, jsonObject.get("some_integer_list").getAsJsonArray().size());
        assertEquals(1, jsonObject.get("some_integer_list").getAsJsonArray().get(0).getAsJsonPrimitive().getAsInt());
        assertEquals(2, jsonObject.get("some_integer_list").getAsJsonArray().get(1).getAsJsonPrimitive().getAsInt());
        assertEquals(3, jsonObject.get("some_integer_list").getAsJsonArray().get(2).getAsJsonPrimitive().getAsInt());
        assertEquals(2, jsonObject.get("some_map").getAsJsonObject().size());
        assertEquals("value1", jsonObject.get("some_map").getAsJsonObject().get("key1").getAsJsonPrimitive().getAsString());
        assertEquals("value2", jsonObject.get("some_map").getAsJsonObject().get("key2").getAsJsonPrimitive().getAsString());
        assertEquals(2, jsonObject.get("nested_map").getAsJsonObject().size());
        assertEquals(2, jsonObject.get("nested_map").getAsJsonObject().get("key1").getAsJsonObject().size());
        assertEquals("value1", jsonObject.get("nested_map").getAsJsonObject().get("key1").getAsJsonObject().get("key11").getAsJsonPrimitive().getAsString());
        assertEquals("value2", jsonObject.get("nested_map").getAsJsonObject().get("key1").getAsJsonObject().get("key12").getAsJsonPrimitive().getAsString());
    }
}
