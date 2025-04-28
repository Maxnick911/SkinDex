plugins {
    kotlin("jvm")// version "2.1.0"
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

//repositories {
//    mavenCentral()
//    gradlePluginPortal()
//}

dependencies {
    implementation("io.ktor:ktor-server-core:3.1.2")
    implementation("io.ktor:ktor-server-netty:3.1.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-jackson:3.1.2")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.61.0")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
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