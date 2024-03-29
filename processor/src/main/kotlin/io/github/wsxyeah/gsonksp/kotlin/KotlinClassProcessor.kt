package io.github.wsxyeah.gsonksp.kotlin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.wsxyeah.gsonksp.serializedName

class KotlinClassProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val ksKotlinPoet: KSKotlinPoet,
) {

    fun processClasses(classDeclarations: Sequence<KSClassDeclaration>) {
        classDeclarations.filter { it.origin == Origin.KOTLIN }
            .forEach {
                logger.warn("processing kotlin class: " + it.qualifiedName?.asString())
                processClass(it)
            }
    }

    private fun processClass(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.packageName.asString()
        val simpleClassName = classDeclaration.simpleName.asString()
        val className = ClassName(packageName, simpleClassName)
        val adapterClassName = ClassName(packageName, simpleClassName + "GsonAdapter")
        val parameterizedTypeAdapterTypeName = GsonKotlinTypeNames.ParameterizedTypeAdapter(className)
        val properties = classDeclaration.getAllProperties()

        val typeAdapterProperties = generateTypeAdapterProperties(className, properties).asIterable()
        val constructor = generateConstructor(className, properties)
        val readFun = generateReadFun(classDeclaration, className, properties)
        val writeFun = generateWriteFun(className, properties)

        val adapterClassSpec = TypeSpec.classBuilder(adapterClassName)
            .superclass(parameterizedTypeAdapterTypeName)
            .addProperty("gson", GsonKotlinTypeNames.Gson, KModifier.PRIVATE)
            .addProperties(typeAdapterProperties)
            .primaryConstructor(constructor)
            .addFunction(readFun)
            .addFunction(writeFun)
            .build()
        val ktFile = FileSpec.builder(adapterClassName)
            .addType(adapterClassSpec)
            .indent("    ")
            .build()

        codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName,
            adapterClassName.simpleName,
            "kt",
        ).bufferedWriter().use(ktFile::writeTo)
    }

    private fun generateReadFun(
        classDeclaration: KSClassDeclaration,
        className: ClassName,
        properties: Sequence<KSPropertyDeclaration>,
    ) = FunSpec.builder("read")
        .addModifiers(KModifier.OVERRIDE)
        .returns(className.copy(nullable = true))
        .addParameter("in", GsonKotlinTypeNames.JsonReader)
        .beginControlFlow("if (`in`.peek() == %T.NULL)", GsonKotlinTypeNames.JsonToken)
        .addStatement("return null")
        .endControlFlow()
        .addStatement("`in`.beginObject()")
        .apply {
            val maskCount = (properties.count() + 31) / 32
            for (i in 0 until maskCount) {
                addStatement("var mask${i} = -1")
            }
            properties.forEach {
                val propertyName = it.simpleName.asString()
                val propertyType = it.type.resolve()
                val propertyTypeName = ksKotlinPoet.toTypeName(propertyType)
                addStatement(
                    "var %L: %T = %L",
                    propertyName,
                    propertyTypeName.copy(nullable = true),
                    ksKotlinPoet.getTypeDefaultValue(propertyType)
                )
            }
        }
        .beginControlFlow("while (`in`.hasNext())")
        .beginControlFlow("when (`in`.nextName())")
        .apply {
            properties.forEachIndexed { index, property ->
                val propertyName = property.simpleName.asString()
                val propertyTypeAdapterName = "$propertyName\$TypeAdapter"
                val serializedName = property.serializedName()

                beginControlFlow("%S ->", serializedName)
                addStatement("%N = this.%N.read(`in`)", propertyName, propertyTypeAdapterName)
                val maskIndex = index / 32
                addStatement("mask${maskIndex} = mask${maskIndex} and (1 shl ${index}).inv()")
                endControlFlow()
            }
            beginControlFlow("else ->")
            addStatement("`in`.skipValue()")
            endControlFlow()
        }
        .endControlFlow()
        .endControlFlow()
        .addStatement("`in`.endObject()")
        .apply {
            val primaryConstructor = classDeclaration.primaryConstructor ?: return@apply
            val constructorHasDefaultArgs = primaryConstructor.parameters.any { it.hasDefault }

            if (constructorHasDefaultArgs) {
                val constructorArgClassesExpr = primaryConstructor.parameters.joinToString(", ") {
                    val paramType = it.type.resolve()
                    CodeBlock.of("%T::class.java", paramType.toTypeName()).toString()
                }
                val constructorArgsExpr = primaryConstructor.parameters.joinToString(", ") {
                    val paramName = it.name!!.asString()
                    CodeBlock.of("%L", paramName).toString()
                }
                val maskCount = (properties.count() + 31) / 32
                val maskClassesExpr = Array(maskCount) { "Int::class.java" }.joinToString(", ")
                val maskArgsExpr = Array(maskCount) { "mask${it / 32}" }.joinToString(", ")

                addStatement("val defaultConstructorMarkerCls = Class.forName(\"kotlin.jvm.internal.DefaultConstructorMarker\")")
                addStatement(
                    "val ctor = %T::class.java.getDeclaredConstructor(" +
                            "${constructorArgClassesExpr}, " +
                            "${maskClassesExpr}, " +
                            "defaultConstructorMarkerCls" +
                            ")",
                    className,
                )
                addStatement("return ctor.newInstance(${constructorArgsExpr}, ${maskArgsExpr}, null)")
            } else {
                val constructorArgs = primaryConstructor.parameters.map {
                    val paramName = it.name!!.asString()
                    CodeBlock.of("%L = %L!!", paramName, paramName).toString()
                }
                addStatement("return %T(%L)", className, constructorArgs.joinToString(", "))
            }
        }
        .build()

    private fun generateWriteFun(className: ClassName, properties: Sequence<KSPropertyDeclaration>): FunSpec =
        FunSpec.builder("write")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("out", GsonKotlinTypeNames.JsonWriter)
            .addParameter("value", className.copy(nullable = true))
            .beginControlFlow("if (value == null)")
            .addStatement("`out`.nullValue()")
            .addStatement("return")
            .endControlFlow()
            .addStatement("`out`.beginObject()")
            .apply {
                properties.forEach {
                    val propertyName = it.simpleName.asString()
                    val propertyTypeAdapterName = "$propertyName\$TypeAdapter"
                    val serializedName = it.serializedName()

                    addStatement("`out`.name(%S)", serializedName)
                    addStatement("this.%N.write(`out`, value.%L)", propertyTypeAdapterName, propertyName)
                }
            }
            .addStatement("`out`.endObject()")
            .build()

    private fun generateConstructor(className: ClassName, properties: Sequence<KSPropertyDeclaration>): FunSpec {
        val constructor = FunSpec.constructorBuilder()
            .addParameter("gson", GsonKotlinTypeNames.Gson)
            .addStatement("this.gson = gson")

        properties.forEach {
            val propertyName = it.simpleName.asString()
            val propertyType = it.type.resolve()
            val propertyTypeName = ksKotlinPoet.toTypeName(propertyType)
            constructor.addStatement(
                "this.%N = gson.getAdapter(%T.get(%L) as %T)",
                "$propertyName\$TypeAdapter",
                GsonKotlinTypeNames.TypeToken,
                ksKotlinPoet.toTypeExpr(propertyType),
                GsonKotlinTypeNames.ParameterizedTypeToken(propertyTypeName),
            )
        }

        return constructor.build()
    }

    private fun generateTypeAdapterProperties(
        className: ClassName,
        properties: Sequence<KSPropertyDeclaration>,
    ): Sequence<PropertySpec> =
        properties.map {
            val propertyName = it.simpleName.asString()
            val propertyType = it.type.resolve()
            val propertyAdapterName = "$propertyName\$TypeAdapter"
            PropertySpec.builder(
                propertyAdapterName,
                GsonKotlinTypeNames.ParameterizedTypeAdapter(ksKotlinPoet.toTypeName(propertyType)),
                KModifier.PRIVATE
            ).build()
        }
}