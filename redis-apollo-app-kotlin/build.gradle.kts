plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

group = "com.redis.om"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation(project(":redis-apollo-shared-model"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("com.redis.om:redis-om-spring:0.9.12-SNAPSHOT")
    annotationProcessor("com.redis.om:redis-om-spring:0.9.12-SNAPSHOT")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.1")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-ollama:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-azure-openai:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-vertex-ai-embedding:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-bedrock:1.0.0-M6")
    implementation("org.springframework.ai:spring-ai-transformers:1.0.0-M6")

    implementation("jakarta.websocket:jakarta.websocket-api:2.1.1")
    implementation("jakarta.websocket:jakarta.websocket-client-api:2.1.1")

    // DJL
    implementation("ai.djl.spring:djl-spring-boot-starter-autoconfigure:0.26")
    implementation("ai.djl.spring:djl-spring-boot-starter-pytorch-auto:0.26")
    implementation("ai.djl.huggingface:tokenizers:0.30.0")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}


