import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "me.vzhilin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.microsoft:z3:4.8.17")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("me.vzhilin.gr.Main")
}

kotlin {
    sourceSets {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
        }
    }
}
