plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.girsang"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot + JavaFX"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database: SQLite
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.6.1.Final")




    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // JavaFX
    implementation("org.openjfx:javafx-controls:21.0.4")
    implementation("org.openjfx:javafx-fxml:21.0.4")


    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")


    // DateTime (Jackson)
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.mindrot:jbcrypt:0.4")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// JavaFX configuration
javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

// Enable JUnit Platform
tasks.test {
    useJUnitPlatform()
}

// ✅ Buat JAR yang runnable (Spring Boot + JavaFX)
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.girsang.server.SpringAppKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Spring Boot jar configuration (fat JAR)
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("cetak-stiker-server.jar") // nama file .jar
    launchScript() // membuat executable script di dalam jar
    mainClass.set("com.girsang.server.SpringAppKt")
}
