package io.github.wsxyeah.gsonksp

import com.google.devtools.ksp.symbol.*

fun KSType.isJavaPrimitive(): Boolean {
    return when (this.declaration.qualifiedName?.asString()) {
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Short",
        "kotlin.Byte",
        "kotlin.Float",
        "kotlin.Double",
        "kotlin.Boolean",
        "kotlin.Char",
        -> nullability == Nullability.NOT_NULL

        else -> false
    }
}

fun KSPropertyDeclaration.serializedName(): String {
    return this.findAnnotation("com.google.gson.annotations", "SerializedName")
        ?.arguments
        ?.firstOrNull() { it.name?.asString() == "value" }
        ?.let { it.value?.toString() }
        ?: this.simpleName.asString()
}

fun KSAnnotated.findAnnotation(packageName: String, shortName: String): KSAnnotation? {
    return this.annotations.find {
        it.shortName.asString() == shortName &&
                it.annotationType.resolve().declaration.packageName.asString() == packageName
    }
}
