package io.github.wsxyeah.gsonksp.java

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.squareup.javapoet.*
import io.github.wsxyeah.gsonksp.isJavaPrimitive
import io.github.wsxyeah.gsonksp.serializedName
import java.io.IOException
import javax.lang.model.element.Modifier

class JavaClassProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val ksJavaPoet: KSJavaPoet,
) {

    fun processClasses(classDeclarations: Sequence<KSClassDeclaration>) {
        val adapterClassNames = mutableListOf<ClassName>()
        classDeclarations
            .filter { it.origin == Origin.JAVA }
            .forEach {
                logger.warn("processing java class: " + it.qualifiedName?.asString())
                val adapterClassName = processClass(it)
                adapterClassNames.add(adapterClassName)
            }
        generateTypeAdapterFactory(adapterClassNames)
    }

    private fun processClass(ksClassDeclaration: KSClassDeclaration): ClassName {
        val packageName = ksClassDeclaration.containingFile!!.packageName.asString()
        val classSimpleName = ksClassDeclaration.simpleName.asString()
        val adapterClassName = ksClassDeclaration.simpleName.asString() + "GsonAdapter"
        val properties = ksClassDeclaration.declarations.filterIsInstance<KSPropertyDeclaration>().toList()

        val className = ClassName.get(packageName, classSimpleName)
        val parameterizedAdapterType = GsonJavaTypeNames.ParameterizedTypeAdapter(className)

        val typeAdapterFields = generateTypeAdapterFields(properties)
        val constructor = generateConstructor(properties)
        val readMethodSpec = generateReadMethod(className, properties)
        val writeMethodSpec = generateWriteMethod(className, properties)

        val adapterTypeSpec = TypeSpec.classBuilder(adapterClassName)
            .addModifiers(Modifier.PUBLIC)
            .superclass(parameterizedAdapterType)
            .addField(GsonJavaTypeNames.Gson, "gson", Modifier.PRIVATE, Modifier.FINAL)
            .addFields(typeAdapterFields)
            .addMethod(constructor)
            .addMethod(readMethodSpec)
            .addMethod(writeMethodSpec)
            .build()

        val javaFile = JavaFile.builder(packageName, adapterTypeSpec)
            .indent("    ")
            .build()

        codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName,
            adapterClassName,
            "java"
        ).bufferedWriter().use(javaFile::writeTo)

        return ClassName.get(packageName, adapterClassName)
    }

    private fun generateTypeAdapterFields(properties: List<KSPropertyDeclaration>): List<FieldSpec> {
        return properties.map { property ->
            val propertyType = property.type.resolve()
            val propertyJavaPoetTypeName = ksJavaPoet.toTypeName(propertyType)
            val fieldAdapterName = "${property.simpleName.asString()}\$Adapter"

            FieldSpec.builder(
                GsonJavaTypeNames.ParameterizedTypeAdapter(propertyJavaPoetTypeName),
                fieldAdapterName,
                Modifier.PRIVATE, Modifier.FINAL
            ).build()
        }
    }

    private fun generateConstructor(properties: List<KSPropertyDeclaration>): MethodSpec {
        val constructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(GsonJavaTypeNames.Gson, "gson")
            .addStatement("this.gson = gson")
        properties.forEach { property ->
            val propertyType = property.type.resolve()
            val propertyJavaPoetTypeName = ksJavaPoet.toTypeName(propertyType)
            val propertyJavaPoetTypeExpr = ksJavaPoet.toTypeExpr(propertyType)
            val fieldAdapterName = "${property.simpleName.asString()}\$Adapter"

            constructor.addStatement(
                "this.\$L = gson.getAdapter((\$T<\$T>)\$T.get(\$L))",
                fieldAdapterName,
                GsonJavaTypeNames.TypeToken,
                propertyJavaPoetTypeName,
                GsonJavaTypeNames.TypeToken,
                propertyJavaPoetTypeExpr,
            )
        }
        return constructor.build()
    }

    private fun generateReadMethod(
        className: ClassName?,
        properties: List<KSPropertyDeclaration>,
    ): MethodSpec = MethodSpec.methodBuilder("read")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(GsonJavaTypeNames.JsonReader, "in")
        .returns(className)
        .addException(IOException::class.java)
        // statements
        .beginControlFlow("if (in.peek() == \$T.NULL)", GsonJavaTypeNames.JsonToken)
        .addStatement("in.nextNull()")
        .addStatement("return null")
        .endControlFlow()
        .addStatement("\$T out = new \$T()", className, className)
        .addStatement("in.beginObject()")
        .beginControlFlow("while (in.hasNext())")
        .beginControlFlow("switch (in.nextName())")
        .apply {
            properties.forEach { property ->
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.resolve()
                val propertyBoxedTypeName = ksJavaPoet.toBoxedClassName(propertyType.declaration)
                val isJavaPrimitive = propertyType.isJavaPrimitive()
                val serializedName = property.serializedName()
                val propertyAdapterName = "$propertyName\$Adapter"
                logger.warn("property[$propertyName]: $propertyType -> $propertyBoxedTypeName")

                beginControlFlow("case \$S:", serializedName)
                addStatement("\$T value = this.\$L.read(in)", propertyBoxedTypeName, propertyAdapterName)
                beginControlFlow("if (value != null || !\$L)", isJavaPrimitive)
                addStatement("out.$propertyName = value")
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

    private fun generateWriteMethod(
        className: ClassName?,
        properties: List<KSPropertyDeclaration>,
    ): MethodSpec = MethodSpec.methodBuilder("write")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(GsonJavaTypeNames.JsonWriter, "out")
        .addParameter(className, "value")
        .addException(IOException::class.java)
        // statements
        .beginControlFlow("if (value == null)")
        .addStatement("out.nullValue()")
        .addStatement("return")
        .endControlFlow()
        .addStatement("out.beginObject()")
        .apply {
            properties.forEach { property ->
                val propertyName = property.simpleName.asString()
                val propertyAdapterName = "$propertyName\$Adapter"
                val serializedName = property.serializedName()
                addStatement("out.name(\$S)", serializedName)
                addStatement("this.\$L.write(out, value.$propertyName)", propertyAdapterName)
            }
        }
        .addStatement("out.endObject()")
        // statements
        .build()

    private fun generateTypeAdapterFactory(adapterClassNames: List<ClassName>) {
        if (adapterClassNames.isEmpty()) {
            return
        }
        logger.warn("generateTypeAdapterFactory: $adapterClassNames")
        val packageName = "io.github.wsxyeah.gsonksp.generated"
        val factoryClassName = "AggregatedTypeAdapterFactory"

        val typeVariableT = TypeVariableName.get("T")
        val createMethodSpec = MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addTypeVariable(typeVariableT)
            .addParameter(GsonJavaTypeNames.Gson, "gson")
            .addParameter(GsonJavaTypeNames.ParameterizedTypeToken(typeVariableT), "type")
            .returns(GsonJavaTypeNames.ParameterizedTypeAdapter(typeVariableT))
            .addStatement("Class rawType = type.getRawType()")
            .addStatement("if (rawType == null) return null")
            .beginControlFlow("switch (rawType.getCanonicalName())")
            .apply {
                adapterClassNames.forEach { adapterClassName ->
                    beginControlFlow("case \$S:", adapterClassName.canonicalName())
                    addStatement("return (TypeAdapter) new \$T(gson)", adapterClassName)
                    endControlFlow()
                }
            }
            .addStatement("default: return null")
            .endControlFlow()
            .build()

        val factoryTypeSpec = TypeSpec.classBuilder(factoryClassName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(GsonJavaTypeNames.TypeAdapterFactory)
            .addMethod(createMethodSpec)
            .build()
        val javaFile = JavaFile.builder(packageName, factoryTypeSpec)
            .indent("    ")
            .build()

        codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName,
            factoryClassName,
            "java"
        ).bufferedWriter().use(javaFile::writeTo)
    }
}