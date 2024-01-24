package io.github.wsxyeah.gsonksp.kotlin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.*

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

        val readFun = FunSpec.builder("read")
            .addModifiers(KModifier.OVERRIDE)
            .returns(className.copy(nullable = true))
            .addParameter("in", GsonKotlinTypeNames.JsonReader)
            .addStatement("TODO(\"not implemented\")")
            .build()

        val writeFun = FunSpec.builder("write")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("out", GsonKotlinTypeNames.JsonWriter)
            .addParameter("value", className.copy(nullable = true))
            .addStatement("TODO(\"not implemented\")")
            .build()

        val adapterClassSpec = TypeSpec.classBuilder(adapterClassName)
            .superclass(parameterizedTypeAdapterTypeName)
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
}