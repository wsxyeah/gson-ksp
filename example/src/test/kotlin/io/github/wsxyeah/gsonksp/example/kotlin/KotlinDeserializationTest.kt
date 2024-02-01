package io.github.wsxyeah.gsonksp.example.kotlin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KotlinDeserializationTest {

    class ItemTypeAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (type.rawType == Item::class.java) {
                return ItemGsonAdapter(gson) as TypeAdapter<T>
            }
            if (type.rawType == ItemWithDefault::class.java) {
                return ItemWithDefaultGsonAdapter(gson) as TypeAdapter<T>
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
    fun `test deserialization null`() {
        val jsonStr = "null"
        val item = gson.fromJson(jsonStr, Item::class.java)
        assertNull(item)
    }

    @Test
    fun `test deserialization`() {
        val jsonStr = """
            {
                "some_int": 20,
                "some_string": "some string",
                "some_boolean": true,
                "some_double": 1.1,
                "some_float": 1.1,
                "some_long": 1,
                "some_short": 1,
                "some_byte": 1,
                "some_char": "a"
            }
        """.trimIndent()
        val item = gson.fromJson(jsonStr, Item::class.java)
        assertNotNull(item)
        assertEquals(20, item.someInt)
        assertEquals("some string", item.someString)
        assertEquals(true, item.someBoolean)
        assertEquals(1.1, item.someDouble, 0.0001)
        assertEquals(1.1f, item.someFloat, 0.0001f)
        assertEquals(1L, item.someLong)
        assertEquals(1.toShort(), item.someShort)
        assertEquals(1.toByte(), item.someByte)
        assertEquals('a', item.someChar)
    }

    @Test
    fun `test deserialization with default value`() {
        val jsonStr = "{\"requiredString\": \"required string\"}"
        val item = gson.fromJson(jsonStr, ItemWithDefault::class.java)
        val defaultItem = ItemWithDefault(requiredString =  "required string")
        assertNotNull(item)
        assertEquals(defaultItem.someInt, item.someInt)
        assertEquals(defaultItem.someString, item.someString)
        assertEquals(defaultItem.someBoolean, item.someBoolean)
        assertEquals(defaultItem.someDouble, item.someDouble, 0.0001)
        assertEquals(defaultItem.someFloat, item.someFloat, 0.0001f)
        assertEquals(defaultItem.someLong, item.someLong)
        assertEquals(defaultItem.someShort, item.someShort)
        assertEquals(defaultItem.someByte, item.someByte)
        assertEquals(defaultItem.someChar, item.someChar)
    }
}