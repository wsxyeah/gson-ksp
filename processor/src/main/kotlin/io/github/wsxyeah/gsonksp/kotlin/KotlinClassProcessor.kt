package io.github.wsxyeah.gsonksp.kotlin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.*
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
        val readFun = generateReadFun(className, properties)
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

    private fun generateReadFun(className: ClassName, properties: Sequence<KSPropertyDeclaration>) =
        FunSpec.builder("read")
            .addModifiers(KModifier.OVERRIDE)
            .returns(className.copy(nullable = true))
            .addParameter("in", GsonKotlinTypeNames.JsonReader)
            .addStatement("TODO(\"not implemented\")")
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