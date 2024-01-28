package io.github.wsxyeah.gsonksp.example.kotlin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KotlinSerializationTest {

    class ItemTypeAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (type.rawType == Item::class.java) {
                return ItemGsonAdapter(gson) as TypeAdapter<T>
            }
            return null
        }
    }

    private lateinit var gson: Gson

    @BeforeEach
    fun setUp() {
        gson = GsonBuilder()
            .registerTypeAdapterFactory(ItemTypeAdapterFactory())
            .create()
    }

    @Test
    fun `test serialization`() {
        val item = Item(
            someInt = 20,
            someString = "some string",
            someBoolean = true,
            someDouble = 1.1,
            someFloat = 1.1f,
            someLong = 1,
            someShort = 1,
            someByte = 1,
            someChar = 'a',
        )
        val jsonElement = gson.toJsonTree(item)
        assertTrue(jsonElement.isJsonObject)
        val jsonObject = jsonElement.asJsonObject
        assertEquals(20, jsonObject.get("some_int").asInt)
        assertEquals("some string", jsonObject.get("some_string").asString)
        assertEquals(true, jsonObject.get("some_boolean").asBoolean)
        assertEquals(1.1, jsonObject.get("some_double").asDouble)
        assertEquals(1.1f, jsonObject.get("some_float").asFloat)
        assertEquals(1L, jsonObject.get("some_long").asLong)
        assertEquals(1.toShort(), jsonObject.get("some_short").asShort)
        assertEquals(1.toByte(), jsonObject.get("some_byte").asByte)
        assertEquals('a', jsonObject.get("some_char").asCharacter)
    }
}