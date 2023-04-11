import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {
	val kotlinVersion = "1.6.10"
	val springBootVersion = "2.7.7"
	repositories {
		mavenLocal()
		mavenCentral()
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
	}
}

plugins {
	id("org.springframework.boot") version "2.7.7"
	id("maven-publish")
	id("org.jetbrains.kotlin.jvm") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"
}

apply(plugin = "maven-publish")
apply(plugin = "io.spring.dependency-management")

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

repositories {
	mavenLocal()
	mavenCentral()
}

version=File(".version").readText(Charsets.UTF_8)

group = "ie.daithi.electricityprices"
java.sourceCompatibility = JavaVersion.VERSION_17

description = "api"

val springBootVersion: String = "2.7.7"
val swaggerVersion: String = "2.9.2"

dependencies {

	// Internal Dependencies

	// External Dependencies

	// Kotlin dependencies
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")

	// Spring dependencies
	implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb:$springBootVersion")

	// Springfox
	implementation("io.springfox:springfox-swagger2:$swaggerVersion")
	implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")

	// Other
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
	implementation("org.apache.commons:commons-text:1.10.0")

	//Test
	testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
	testImplementation("io.mockk:mockk:1.13.4")


}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.getByName<BootJar>("bootJar") {
	enabled = true
}

tasks.getByName<Jar>("jar") {
	enabled = false
}
