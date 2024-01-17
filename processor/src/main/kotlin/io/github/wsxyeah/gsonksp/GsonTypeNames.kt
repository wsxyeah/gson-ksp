package io.github.wsxyeah.gsonksp

import com.squareup.javapoet.ClassName

object GsonTypeNames {
    val TypeAdapter = ClassName.get("com.google.gson", "TypeAdapter")
    val TypeToken = ClassName.get("com.google.gson.reflect", "TypeToken")
    val GsonTypes = ClassName.get("com.google.gson.internal", "\$Gson\$Types")
}
