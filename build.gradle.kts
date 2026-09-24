import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.3.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(libs.com.fasterxml.jackson.core.jackson.databind)
    compileOnly(libs.io.papermc.paper.paper.api)
}

val buildNumberFile = file("build-number.txt")
val buildNumber: Int = if (buildNumberFile.exists()) {
    val currentBuildNumber = buildNumberFile.readText().trim().toInt()
    buildNumberFile.writeText("${currentBuildNumber + 1}")
    currentBuildNumber + 1
} else {
    buildNumberFile.writeText("1")
    1
}

group = "gruvexp"
version = "1.0.0-$buildNumber"
description = "The plugin used on the tribes server"
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register("incrementBuildNumber") {
    description = "makes the buildnumber go up"
    doLast {
        val currentBuildNumber = buildNumberFile.readText().trim().toInt()
        val newBuildNumber = currentBuildNumber + 1
        buildNumberFile.writeText("$newBuildNumber")
        println("Build number incremented to $newBuildNumber")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveVersion.set("")
    archiveClassifier.set("")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
