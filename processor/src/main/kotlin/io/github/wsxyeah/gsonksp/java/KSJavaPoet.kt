package io.github.wsxyeah.gsonksp.java

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName

class KSJavaPoet(
    private val resolver: Resolver,
) {
    fun toTypeName(type: KSType): TypeName {
        val rawTypeName = toBoxedClassName(type.declaration)
        if (type.arguments.isEmpty()) {
            return rawTypeName
        }
        val argumentTypeNameArr = type.arguments.map { argument ->
            toTypeName(argument.type!!.resolve())
        }.toTypedArray()
        return ParameterizedTypeName.get(rawTypeName, *argumentTypeNameArr)
    }

    fun toTypeExpr(type: KSType): CodeBlock {
        val rawTypeName = toBoxedClassName(type.declaration)
        val rawTypeExpr = CodeBlock.of("\$T.class", rawTypeName)
        if (type.arguments.isEmpty()) {
            return rawTypeExpr
        }

        val argumentsPlaceholder = type.arguments.joinToString(", ") { "\$L" }
        val argumentTypeNameExprArr = type.arguments.map { argument ->
            toTypeExpr(argument.type!!.resolve())
        }.toTypedArray()
        return CodeBlock.of(
            "\$T.newParameterizedTypeWithOwner(null, \$L, $argumentsPlaceholder)",
            GsonJavaTypeNames.GsonTypes,
            rawTypeExpr,
            *argumentTypeNameExprArr,
        )
    }

    @OptIn(KspExperimental::class)
    fun toBoxedClassName(declaration: KSDeclaration): ClassName {
        val fqName = declaration.qualifiedName ?: return ClassName.OBJECT
        val javaName = resolver.mapKotlinNameToJava(fqName) ?: fqName
        return ClassName.get(javaName.getQualifier(), javaName.getShortName())
    }
}