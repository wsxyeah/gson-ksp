package io.github.wsxyeah.gsonksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.squareup.javapoet.*
import java.io.IOException
import javax.lang.model.element.Modifier

class GsonSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    companion object {
        private const val GENERATE_GSON_ADAPTER_ANNOTATION = "io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val adapterClassNames = mutableListOf<ClassName>()
        val ksJavaPoet = KSJavaPoet(resolver)
        resolver.getSymbolsWithAnnotation(GENERATE_GSON_ADAPTER_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.origin == Origin.JAVA }
            .forEach {
                logger.warn("processing class: " + it.qualifiedName?.asString())
                val adapterClassName = processClass(it, ksJavaPoet)
                adapterClassNames.add(adapterClassName)
            }
        generateTypeAdapterFactory(adapterClassNames)

        return emptyList()
    }

    private fun processClass(
        it: KSClassDeclaration,
        ksJavaPoet: KSJavaPoet,
    ): ClassName {
        val packageName = it.containingFile!!.packageName.asString()
        val classSimpleName = it.simpleName.asString()
        val adapterClassName = it.simpleName.asString() + "GsonAdapter"
        val properties = it.declarations.filterIsInstance<KSPropertyDeclaration>().toList()

        val className = ClassName.get(packageName, classSimpleName)
        val parameterizedAdapterType = GsonTypeNames.ParameterizedTypeAdapter(className)

        val readMethodSpec = MethodSpec.methodBuilder("read")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(GsonTypeNames.JsonReader, "in")
            .returns(className)
            .addException(IOException::class.java)
            // statements
            .beginControlFlow("if (in.peek() == \$T.NULL)", GsonTypeNames.JsonToken)
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

        val writeMethodSpec = MethodSpec.methodBuilder("write")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addParameter(GsonTypeNames.JsonWriter, "out")
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

        val constructorBuilder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(GsonTypeNames.Gson, "gson")
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
                        GsonTypeNames.ParameterizedTypeAdapter(propertyJavaPoetTypeName),
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
            .addField(GsonTypeNames.Gson, "gson", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(constructorBuilder.build())
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
            .addParameter(GsonTypeNames.Gson, "gson")
            .addParameter(GsonTypeNames.ParameterizedTypeToken(typeVariableT), "type")
            .returns(GsonTypeNames.ParameterizedTypeAdapter(typeVariableT))
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
            .addSuperinterface(GsonTypeNames.TypeAdapterFactory)
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
