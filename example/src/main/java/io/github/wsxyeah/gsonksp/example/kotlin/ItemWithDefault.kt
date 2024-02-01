package io.github.wsxyeah.gsonksp.example.kotlin

import io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter

@GenerateGsonAdapter
class ItemWithDefault(
    val requiredString: String,
    val someInt: Int = 30,
    val someString: String = "default string",
    val someBoolean: Boolean = false,
    val someDouble: Double = 50.0,
    val someFloat: Float = 60.0f,
    val someLong: Long = 200L,
    val someShort: Short = 20,
    val someByte: Byte = 3,
    val someChar: Char = 6.toChar(),
)