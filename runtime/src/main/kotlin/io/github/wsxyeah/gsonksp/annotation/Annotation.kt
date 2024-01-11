package io.github.wsxyeah.gsonksp.annotation

@Target(AnnotationTarget.CLASS)
annotation class GenerateGsonAdapter()

@Target(AnnotationTarget.FUNCTION)
annotation class GsonSetter(val name: String)
