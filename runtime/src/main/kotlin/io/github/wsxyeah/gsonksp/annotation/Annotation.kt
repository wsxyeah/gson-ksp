package io.github.wsxyeah.gsonksp.annotation

@Target(AnnotationTarget.CLASS)
annotation class GenerateGsonAdapter()

@Target(AnnotationTarget.PROPERTY)
annotation class GsonSetter(val name: String)
