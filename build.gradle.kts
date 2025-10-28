plugins {
    java
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.ureca"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot - using WebFlux for reactive support
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Cosmos DB Java SDK v4
    implementation("com.azure:azure-cosmos:4.53.1")
    
    // Azure Identity for DefaultAzureCredential
    implementation("com.azure:azure-identity:1.11.1")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Lombok for cleaner code
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // SpringDoc OpenAPI (Swagger UI) - for development
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
