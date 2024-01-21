package io.github.wsxyeah.gsonksp

import com.squareup.javapoet.ClassName

object GsonTypeNames {
    val Gson: ClassName = ClassName.get("com.google.gson", "Gson")
    val TypeAdapter: ClassName = ClassName.get("com.google.gson", "TypeAdapter")
    val TypeAdapterFactory: ClassName = ClassName.get("com.google.gson", "TypeAdapterFactory")
    val TypeToken: ClassName = ClassName.get("com.google.gson.reflect", "TypeToken")
    val GsonTypes: ClassName = ClassName.get("com.google.gson.internal", "\$Gson\$Types")
}
