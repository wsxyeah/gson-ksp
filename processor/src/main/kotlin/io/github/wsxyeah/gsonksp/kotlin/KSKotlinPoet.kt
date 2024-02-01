package io.github.wsxyeah.gsonksp.kotlin

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName

class KSKotlinPoet(
    private val resolver: Resolver,
) {
    fun toTypeName(type: KSType): TypeName {
        val rawTypeName = type.toClassName()
        if (type.arguments.isEmpty()) {
            return rawTypeName
        }
        val argumentTypeNameArr = type.arguments.map { argument ->
            toTypeName(argument.type!!.resolve())
        }.toTypedArray()
        return rawTypeName.parameterizedBy(*argumentTypeNameArr)
    }

    fun toTypeExpr(type: KSType): CodeBlock {
        val rawTypeName = type.toClassName()
        val rawTypeExpr = CodeBlock.of("%T::class.java", rawTypeName)
        if (type.arguments.isEmpty()) {
            return rawTypeExpr
        }

        val argumentsPlaceholder = type.arguments.joinToString(", ") { "%L" }
        val argumentTypeNameExprArr = type.arguments.map { argument ->
            toTypeExpr(argument.type!!.resolve())
        }.toTypedArray()
        return CodeBlock.of(
            "%T.newParameterizedTypeWithOwner(null, %L, $argumentsPlaceholder)",
            GsonKotlinTypeNames.GsonTypes,
            rawTypeExpr,
            *argumentTypeNameExprArr,
        )
    }

    fun getTypeDefaultValue(type: KSType): CodeBlock {
        return when (type) {
            resolver.builtIns.booleanType -> CodeBlock.of("false")
            resolver.builtIns.byteType -> CodeBlock.of("0")
            resolver.builtIns.charType -> CodeBlock.of("0.toChar()")
            resolver.builtIns.doubleType -> CodeBlock.of("0.0")
            resolver.builtIns.floatType -> CodeBlock.of("0.0f")
            resolver.builtIns.intType -> CodeBlock.of("0")
            resolver.builtIns.longType -> CodeBlock.of("0")
            resolver.builtIns.shortType -> CodeBlock.of("0")
            else -> CodeBlock.of("null")
        }
    }
}