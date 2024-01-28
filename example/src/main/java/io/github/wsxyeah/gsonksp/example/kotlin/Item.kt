package io.github.wsxyeah.gsonksp.example.kotlin

import com.google.gson.annotations.SerializedName
import io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter

@GenerateGsonAdapter
class Item(
    @SerializedName("some_int")
    val someInt: Int,
    @SerializedName("some_string")
    val someString: String,
    @SerializedName("some_boolean")
    val someBoolean: Boolean,
    @SerializedName("some_double")
    val someDouble: Double,
    @SerializedName("some_float")
    val someFloat: Float,
    @SerializedName("some_long")
    val someLong: Long,
    @SerializedName("some_short")
    val someShort: Short,
    @SerializedName("some_byte")
    val someByte: Byte,
    @SerializedName("some_char")
    val someChar: Char,
)