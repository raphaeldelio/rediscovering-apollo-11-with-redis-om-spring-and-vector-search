plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("kapt") version "1.9.25"
    id("org.springframework.boot") version "3.5.7"
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
    mavenCentral()
}

extra["springAiVersion"] = "1.1.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("com.redis.om:redis-om-spring:1.1.0")
    implementation("com.redis.om:redis-om-spring-ai:1.1.0")
    kapt("com.redis.om:redis-om-spring:1.1.0")
    implementation("com.redis:redisvl:0.0.1")
    implementation("redis.clients:jedis:7.0.0")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.4.1")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-ollama")
    implementation("org.springframework.ai:spring-ai-bedrock")
    implementation("org.springframework.ai:spring-ai-transformers")

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

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "redis.clients" && requested.name == "jedis") {
            useVersion("7.0.0")
            because("RedisVL and Redis OM require UnifiedJedis.pipelined() from Jedis 6.x+")
        }
    }
}
