package io.github.wsxyeah.gsonksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import javax.lang.model.element.Modifier

class GsonSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val ksJavaPoet = KSJavaPoet(resolver)
        resolver.getSymbolsWithAnnotation("io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter")
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.origin == Origin.JAVA }
            .forEach {
                logger.warn("annotated: " + it + "<" + it.javaClass + ">")

                val properties = it.declarations.filterIsInstance<KSPropertyDeclaration>().toList()
                properties.forEach { property ->
                    logger.warn("property: $property")
                }

                val packageName = it.containingFile!!.packageName.asString()
                val classSimpleName = it.simpleName.asString()
                val adapterClassName = it.simpleName.asString() + "GsonAdapter"
                val fileOutput = codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    packageName,
                    adapterClassName,
                    "java"
                ).bufferedWriter()

                val className = ClassName.get(packageName, classSimpleName)
                val gsonClass = ClassName.get("com.google.gson", "Gson")
                val typeAdapterClass = ClassName.get("com.google.gson", "TypeAdapter")
                val parameterizedAdapterType = ParameterizedTypeName.get(typeAdapterClass, className)
                val jsonReaderClass = ClassName.get("com.google.gson.stream", "JsonReader")
                val jsonWriterClass = ClassName.get("com.google.gson.stream", "JsonWriter")
                val jsonTokenClass = ClassName.get("com.google.gson.stream", "JsonToken")

                val readMethodSpec = MethodSpec.methodBuilder("read")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .addParameter(jsonReaderClass, "in")
                    .returns(className)
                    .addException(IOException::class.java)
                    // statements
                    .beginControlFlow("if (in.peek() == \$T.NULL)", jsonTokenClass)
                    .addStatement("in.nextNull()")
                    .addStatement("return null")
                    .endControlFlow()
                    .addStatement("\$T out = new \$T()", className, className)
                    .addStatement("in.beginObject()")
                    .beginControlFlow("while (in.hasNext())")
                    .beginControlFlow("switch (in.nextName())")
                    .apply {
                        properties.forEach { property ->
                            val serializedName = property.serializedName()
                            val propertyType = property.type.resolve()
                            val propertyBoxedTypeName = ksJavaPoet.toBoxedClassName(propertyType.declaration)
                            val isJavaPrimitive = propertyType.isJavaPrimitive()
                            logger.warn("property[${property.simpleName}]: $propertyType -> $propertyBoxedTypeName")

                            beginControlFlow("case \$S:", serializedName)
                            val propertyAdapterName = "${property.simpleName.asString()}\$Adapter"
                            addStatement("\$T value = this.\$L.read(in)", propertyBoxedTypeName, propertyAdapterName)
                            beginControlFlow("if (value != null || !\$L)", isJavaPrimitive)
                            addStatement("out.${property.simpleName.asString()} = value")
                            endControlFlow()
                            addStatement("break")
                            endControlFlow()
                        }

                        beginControlFlow("default:")
                        addStatement("in.skipValue()")
                        addStatement("break")
                        endControlFlow()
                    }
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("in.endObject()")
                    .addStatement("return out")
                    // statements
                    .build()

                val writeMethodSpec = MethodSpec.methodBuilder("write")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .addParameter(jsonWriterClass, "out")
                    .addParameter(className, "value")
                    .addException(IOException::class.java)
                    // statements
                    .addStatement("return")
                    // statements
                    .build()

                val constructorBuilder = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(gsonClass, "gson")
                    .addStatement("this.gson = gson")

                val adapterTypeSpec = TypeSpec.classBuilder(adapterClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(parameterizedAdapterType)
                    .apply {
                        properties.forEach { property ->
                            val propertyType = property.type.resolve()
                            val propertyJavaPoetTypeName = ksJavaPoet.toTypeName(propertyType)
                            val propertyJavaPoetTypeExpr = ksJavaPoet.toTypeExpr(propertyType)
                            val fieldAdapterName = "${property.simpleName.asString()}\$Adapter"

                            logger.warn("fieldAdapter: $fieldAdapterName, $propertyJavaPoetTypeName, $propertyJavaPoetTypeExpr")
                            addField(
                                ParameterizedTypeName.get(GsonTypeNames.TypeAdapter, propertyJavaPoetTypeName),
                                fieldAdapterName,
                                Modifier.PRIVATE, Modifier.FINAL
                            )
                            constructorBuilder.addStatement(
                                "this.\$L = gson.getAdapter((\$T<\$T>)\$T.get(\$L))",
                                fieldAdapterName,
                                GsonTypeNames.TypeToken,
                                propertyJavaPoetTypeName,
                                GsonTypeNames.TypeToken,
                                propertyJavaPoetTypeExpr,
                            )
                        }
                    }
                    .addField(ClassName.get("com.google.gson", "Gson"), "gson", Modifier.PRIVATE, Modifier.FINAL)
                    .addMethod(constructorBuilder.build())
                    .addMethod(readMethodSpec)
                    .addMethod(writeMethodSpec)
                    .build()

                JavaFile.builder(packageName, adapterTypeSpec)
                    .indent("    ")
                    .build()
                    .writeTo(fileOutput)

                fileOutput.flush()
            }

        return emptyList()
    }

    private fun KSType.isJavaPrimitive(): Boolean {
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

    private fun KSPropertyDeclaration.serializedName(): String {
        return this.findAnnotation("com.google.gson.annotations", "SerializedName")
            ?.arguments
            ?.firstOrNull() { it.name?.asString() == "value" }
            ?.let { it.value?.toString() }
            ?: this.simpleName.asString()
    }

    private fun KSAnnotated.findAnnotation(packageName: String, shortName: String): KSAnnotation? {
        return this.annotations.find {
            it.shortName.asString() == shortName &&
                    it.annotationType.resolve().declaration.packageName.asString() == packageName
        }
    }

}