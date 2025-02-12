val exposed_version: String by project
val h2_version: String by project
val kotlin_version: String by project

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "top.qwq2333"
version = "0.0.1"

application {
    mainClass.set("top.qwq2333.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://packages.confluent.io/maven/") }
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("com.google.guava:guava:33.4.0-jre")

    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.1")
    implementation("com.h2database:h2:$h2_version")

    implementation("com.google.code.gson:gson:2.12.1")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:0.90.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
    implementation("co.nstant.in:cbor:0.9")

    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
