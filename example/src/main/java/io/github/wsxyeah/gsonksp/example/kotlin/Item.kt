package io.github.wsxyeah.gsonksp.example.kotlin

import io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter

@GenerateGsonAdapter
class Item(
    val someInt: Int,
    val someString: String,
    val someBoolean: Boolean,
    val someDouble: Double,
    val someFloat: Float,
    val someLong: Long,
    val someShort: Short,
    val someByte: Byte,
    val someChar: Char,
)