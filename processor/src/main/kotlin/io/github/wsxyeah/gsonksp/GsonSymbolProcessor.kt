package io.github.wsxyeah.gsonksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.wsxyeah.gsonksp.java.JavaClassProcessor
import io.github.wsxyeah.gsonksp.java.KSJavaPoet
import io.github.wsxyeah.gsonksp.kotlin.KSKotlinPoet
import io.github.wsxyeah.gsonksp.kotlin.KotlinClassProcessor

class GsonSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    companion object {
        private const val GENERATE_GSON_ADAPTER_ANNOTATION = "io.github.wsxyeah.gsonksp.annotation.GenerateGsonAdapter"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classDeclarations = resolver.getSymbolsWithAnnotation(GENERATE_GSON_ADAPTER_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()

        val ksJavaPoet = KSJavaPoet(resolver)
        val javaClassProcessor = JavaClassProcessor(codeGenerator, logger, ksJavaPoet)
        javaClassProcessor.processClasses(classDeclarations)

        val ksKotlinPoet = KSKotlinPoet(resolver)
        val kotlinClassProcessor = KotlinClassProcessor(codeGenerator, logger, ksKotlinPoet)
        kotlinClassProcessor.processClasses(classDeclarations)

        return emptyList()
    }
}
