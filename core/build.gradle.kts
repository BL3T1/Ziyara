plugins {
	java
	id("org.springframework.boot") version "3.5.12"
	id("nu.studer.jooq") version "9.0"
}

group = "com.ziyara"
version = "1.0.0"
description = "Demo project for Spring Boot"

java {
	// Bytecode 17 (matches Docker image). Build with any JDK 17+ on the host (no strict toolchain).
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(17)
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
	all {
		resolutionStrategy.eachDependency {
			// Some starters (e.g. older springdoc) can still pull Spring Boot 4.x; align everything on the chosen baseline.
			if (requested.group == "org.springframework.boot") {
				useVersion("3.5.12")
				because("Project targets Spring Boot 3.5.12 (Java 17)")
			}
			// Snyk: SNYK-JAVA-COMFASTERXMLJACKSONCORE-15365924 (CWE-770) — align with jackson-bom 2.21.x
			// (jackson-annotations is released as 2.21 only; core/databind use 2.21.1 per FasterXML BOM.)
			if (requested.group == "com.fasterxml.jackson.core") {
				val v = if (requested.name == "jackson-annotations") "2.21" else "2.21.1"
				useVersion(v)
				because("Jackson security alignment (jackson-bom 2.21.1)")
			}
			if (requested.group.startsWith("com.fasterxml.jackson.datatype")
				|| requested.group.startsWith("com.fasterxml.jackson.module")
				|| requested.group.startsWith("com.fasterxml.jackson.dataformat")
				|| requested.group.startsWith("com.fasterxml.jackson.jaxrs")
				|| requested.group.startsWith("com.fasterxml.jackson.jakarta.rs")
				|| requested.group.startsWith("com.fasterxml.jackson.jr")
			) {
				useVersion("2.21.1")
				because("Jackson security alignment with jackson-core 2.21.1")
			}
			// Snyk: CVE-2024-25710 / CVE-2024-26308 (commons-compress via transitive deps).
			if (requested.group == "org.apache.commons" && requested.name == "commons-compress") {
				useVersion("1.26.0")
				because("CVE-2024-25710 / CVE-2024-26308")
			}
		}
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Force consistent Spring Boot dependency versions (prevents accidental Boot 4.x resolution)
	implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.12"))
	testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.12"))

	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	// Flyway — versioned DB migrations (flyway-database-postgresql required for Flyway 10+ PostgreSQL support)
	implementation("org.flywaydb:flyway-core")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-security")
	// Custom JWT (JwtAuthenticationFilter); oauth2-resource-server would register BearerToken handling and conflict.
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-cache")
	implementation("io.micrometer:micrometer-tracing-bridge-brave")
	implementation("net.logstash.logback:logstash-logback-encoder:8.0")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.springframework.security:spring-security-web")
	// JWT (jjwt) - backend auth
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
	// Spring Boot 3.5.x: springdoc 2.6.x is not compatible (OpenAPI generation / runtime errors).
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.16")
	
	// AWS S3 SDK v2 (optional — only loaded when APP_MEDIA_STORAGE_BACKEND=s3)
	implementation(platform("software.amazon.awssdk:bom:2.26.29"))
	implementation("software.amazon.awssdk:s3")

	// Report exports: Excel (Apache POI) and PDF (OpenPDF)
	implementation("org.apache.poi:poi-ooxml:5.2.5")
	implementation("com.github.librepdf:openpdf:1.3.35")

	compileOnly("org.projectlombok:lombok:1.18.38")
	annotationProcessor("org.projectlombok:lombok:1.18.38")
	runtimeOnly("org.postgresql:postgresql")
	implementation("commons-codec:commons-codec:1.17.1")
	// Optional strength scoring (0–4); disabled when ziyara.password-policy.min-zxcvbn-score is 0.
	implementation("com.nulab-inc:zxcvbn:1.9.0")

	testCompileOnly("org.projectlombok:lombok:1.18.38")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter:1.19.8")
	testImplementation("org.testcontainers:postgresql:1.19.8")
	testImplementation("org.testcontainers:kafka:1.19.8")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform {
		// Testcontainers tests are tagged "docker". Run: gradlew test -PrunDockerTests (Docker required).
		if (!project.hasProperty("runDockerTests")) {
			excludeTags("docker")
		}
	}
}

springBoot {
	mainClass.set("com.ziyara.backend.ZiyarahApplication")
}