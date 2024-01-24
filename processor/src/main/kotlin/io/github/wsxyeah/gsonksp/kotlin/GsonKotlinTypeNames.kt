package io.github.wsxyeah.gsonksp.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName


object GsonKotlinTypeNames {
    val Gson: ClassName = ClassName("com.google.gson", "Gson")
    val TypeAdapter: ClassName = ClassName("com.google.gson", "TypeAdapter")
    val TypeAdapterFactory: ClassName = ClassName("com.google.gson", "TypeAdapterFactory")
    val TypeToken: ClassName = ClassName("com.google.gson.reflect", "TypeToken")
    val GsonTypes: ClassName = ClassName("com.google.gson.internal", "\$Gson\$Types")

    val JsonReader: ClassName = ClassName("com.google.gson.stream", "JsonReader")
    val JsonWriter: ClassName = ClassName("com.google.gson.stream", "JsonWriter")
    val JsonToken: ClassName = ClassName("com.google.gson.stream", "JsonToken")

    fun ParameterizedTypeAdapter(type: TypeName): ParameterizedTypeName = TypeAdapter.parameterizedBy(type)
    fun ParameterizedTypeToken(type: TypeName): ParameterizedTypeName = TypeToken.parameterizedBy(type)
}
