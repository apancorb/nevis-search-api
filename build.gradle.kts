plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.nevis"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.1.4")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring AI - ONNX local embeddings (no API key needed)
    implementation("org.springframework.ai:spring-ai-starter-model-transformers")

    // Spring AI - pgvector vector store
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")

    // Spring AI - OpenAI (library only, no auto-config — we configure manually when API key is present)
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-client-chat")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.rest-assured:rest-assured")
}

tasks.register<Exec>("startTestDb") {
    commandLine("docker", "compose", "up", "postgres", "-d", "--wait")
}

tasks.register<Exec>("stopTestDb") {
    commandLine("docker", "compose", "down")
    isIgnoreExitValue = true
}

tasks.withType<Test> {
    useJUnitPlatform()
    dependsOn("startTestDb")
    finalizedBy("stopTestDb")
    doFirst {
        val process = ProcessBuilder(
            "docker", "inspect", "-f",
            "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}",
            "nevis-search-api-postgres-1"
        ).start()
        val dbHost = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        environment("DB_HOST", dbHost)
    }
}
