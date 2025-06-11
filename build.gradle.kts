plugins {
    kotlin("jvm") version "2.0.10"
    `java-library`
    `maven-publish`
}

group = "org.darchest"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.darchest:insight:1.0-SNAPSHOT")

    implementation("org.postgresql:postgresql:42.7.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories { mavenLocal() }
}