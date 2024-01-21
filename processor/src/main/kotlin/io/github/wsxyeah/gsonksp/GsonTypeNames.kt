package io.github.wsxyeah.gsonksp

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

object GsonTypeNames {
    val Gson: ClassName = ClassName.get("com.google.gson", "Gson")
    val TypeAdapter: ClassName = ClassName.get("com.google.gson", "TypeAdapter")
    val TypeAdapterFactory: ClassName = ClassName.get("com.google.gson", "TypeAdapterFactory")
    val TypeToken: ClassName = ClassName.get("com.google.gson.reflect", "TypeToken")
    val GsonTypes: ClassName = ClassName.get("com.google.gson.internal", "\$Gson\$Types")

    val JsonReader: ClassName = ClassName.get("com.google.gson.stream", "JsonReader")
    val JsonWriter: ClassName = ClassName.get("com.google.gson.stream", "JsonWriter")
    val JsonToken: ClassName = ClassName.get("com.google.gson.stream", "JsonToken")

    fun ParameterizedTypeAdapter(type: TypeName): ParameterizedTypeName = ParameterizedTypeName.get(TypeAdapter, type)
    fun ParameterizedTypeToken(type: TypeName): ParameterizedTypeName = ParameterizedTypeName.get(TypeToken, type)
}
