plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
}

group = "io.github.wsxyeah"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":runtime"))
    ksp(project(":processor"))

    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}