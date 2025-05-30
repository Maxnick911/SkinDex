plugins {
    kotlin("jvm")
    id("application")
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.io.ktor.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.java.jwt)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.dao)
    implementation(libs.postgresql)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jbcrypt)
    implementation(libs.flyway.core)
    implementation(libs.testng)
    implementation(libs.jupiter.junit.jupiter)
    testImplementation(libs.kotlin.test)
    implementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("com.example.backend.ApplicationKt")
}

tasks {
    shadowJar {
        archiveFileName.set("backend.jar")
        manifest {
            attributes("Main-Class" to "com.example.backend.ApplicationKt")
        }
    }

    jar {
        enabled = false
    }

    distZip {
        dependsOn(shadowJar)
    }

    distTar {
        dependsOn(shadowJar)
    }

    startScripts {
        dependsOn(shadowJar)
    }

    build {
        dependsOn(shadowJar)
    }
}