plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "net.proselyte.demo"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["resilience4jVersion"] = "2.4.0"
extra["testcontainersVersion"] = "1.21.3"
extra["wiremockVersion"] = "3.13.1"
extra["archunitVersion"] = "1.4.1"
extra["logstashEncoderVersion"] = "8.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("net.logstash.logback:logstash-logback-encoder:${property("logstashEncoderVersion")}")
    implementation("io.github.resilience4j:resilience4j-core:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-bulkhead:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-timelimiter:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-micrometer:${property("resilience4jVersion")}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation(platform("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.wiremock:wiremock-standalone:${property("wiremockVersion")}")
    testImplementation("com.tngtech.archunit:archunit-junit5:${property("archunitVersion")}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.release = 25
}

springBoot {
    buildInfo()
}

jacoco {
    toolVersion = "0.8.13"
}
