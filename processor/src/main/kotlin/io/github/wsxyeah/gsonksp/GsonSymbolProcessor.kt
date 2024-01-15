package io.github.wsxyeah.gsonksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.javapoet.ClassName
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

        resolver.getSymbolsWithAnnotation("io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter")
            .filterIsInstance<KSClassDeclaration>()
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
                            val propertyType = property.type.resolve()
                            val propertyBoxedTypeName = propertyType.declaration.toBoxedJavaPoetTypeName(resolver)
                            val isJavaPrimitive = propertyType.isJavaPrimitive()
                            val setterMethodName = "set${property.simpleName.asString().capitalize()}"

                            logger.warn("property type: $propertyType -> $propertyBoxedTypeName")
                            beginControlFlow("case \"${property.simpleName.asString()}\":")
                            addStatement(
                                "TypeAdapter<\$T> typeAdapter = gson.getAdapter(\$T.class)",
                                propertyBoxedTypeName,
                                propertyBoxedTypeName,
                            )
                            addStatement("\$T value = typeAdapter.read(in)", propertyBoxedTypeName)
                            beginControlFlow("if (value != null || !\$L)", isJavaPrimitive)
                            addStatement("out.${setterMethodName}(value)")
                            endControlFlow()
                            addStatement("break")
                            endControlFlow()
                        }
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

                val constructorSpec = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(gsonClass, "gson")
                    .addStatement("this.gson = gson")
                    .build()

                val adapterTypeSpec = TypeSpec.classBuilder(adapterClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(parameterizedAdapterType)
                    .addField(ClassName.get("com.google.gson", "Gson"), "gson", Modifier.PRIVATE)
                    .addMethod(constructorSpec)
                    .addMethod(readMethodSpec)
                    .addMethod(writeMethodSpec)
                    .build()
                logger.warn("source: " + JavaFile.builder(packageName, adapterTypeSpec).build().toString())

                JavaFile.builder(packageName, adapterTypeSpec)
                    .indent("    ")
                    .build()
                    .writeTo(fileOutput)

                fileOutput.flush()
            }

        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun KSDeclaration.toBoxedJavaPoetTypeName(resolver: Resolver): TypeName {
        val fqName = this.qualifiedName ?: return ClassName.OBJECT
        val javaName = resolver.mapKotlinNameToJava(fqName) ?: fqName
        return ClassName.get(javaName.getQualifier(), javaName.getShortName())
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


    private fun KSPropertyDeclaration.readerMethodName(): String {
        return when (val type = this.type.resolve().declaration.qualifiedName?.asString()) {
            "kotlin.String" -> "nextString"
            "kotlin.Int" -> "nextInt"
            "kotlin.Long" -> "nextLong"
            "kotlin.Short" -> "nextShort"
            "kotlin.Byte" -> "nextByte"
            "kotlin.Float" -> "nextDouble"
            "kotlin.Double" -> "nextDouble"
            "kotlin.Boolean" -> "nextBoolean"
            else -> throw IllegalArgumentException("unsupported type: $type")
        }
    }

}