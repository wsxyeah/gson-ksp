package io.github.wsxyeah.gsonksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
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
                            beginControlFlow("case \"${property.simpleName.asString()}\":")
                            addStatement("out.set${property.simpleName.asString().capitalize()}((${property.castType()}) in.${property.readerMethodName()}())")
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

    private fun KSPropertyDeclaration.castType(): String {
        return when (val type = this.type.resolve().declaration.qualifiedName!!.asString()) {
            "kotlin.String" -> "String"
            "kotlin.Int" -> "int"
            "kotlin.Long" -> "long"
            "kotlin.Short" -> "short"
            "kotlin.Byte" -> "byte"
            "kotlin.Float" -> "float"
            "kotlin.Double" -> "double"
            "kotlin.Boolean" -> "boolean"
            else -> throw IllegalArgumentException("unsupported type: $type")
        }
    }

    private fun KSPropertyDeclaration.readerMethodName(): String {
        return when (val type = this.type.resolve().declaration.qualifiedName!!.asString()) {
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