plugins {
	java
	jacoco
	id("org.springframework.boot") version "3.5.12"
	id("nu.studer.jooq") version "9.0"
}

// SpringBootAotPlugin is no longer auto-applied by the main Spring Boot plugin (3.4+).
// Applying it explicitly registers the processAot / processTestAot tasks and wires
// the generated bean-factory classes into bootJar's classpath automatically.
apply(plugin = "org.springframework.boot.aot")

group = "com.ziyara"
version = "1.0.0"
description = "Demo project for Spring Boot"

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
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
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("io.micrometer:micrometer-tracing-bridge-brave")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
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
	// Rate limiting: Bucket4j core + Redis-backed distributed proxy manager (Lettuce)
	implementation("com.bucket4j:bucket4j-core:8.10.1")
	implementation("com.bucket4j:bucket4j-redis:8.10.1")
	// Caffeine bounded cache (used in Bucket4jRateLimitAspect and CacheConfig.localCacheManager).
	// spring-boot-starter-cache provides CaffeineCacheManager from spring-context-support but
	// does NOT transitively include the Caffeine library itself — it must be declared explicitly.
	implementation("com.github.ben-manes.caffeine:caffeine")

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

	// H2 is only used during AOT build-time processing (processAot forks a JVM with an in-process DB).
	// It is present in the final jar but never activated in production — no H2 URL is configured there.
	runtimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform {
		// Testcontainers tests are tagged "docker". Run: gradlew test -PrunDockerTests (Docker required).
		if (!project.hasProperty("runDockerTests")) {
			excludeTags("docker")
		}
	}
	// Emit JUnit XML so CI can parse test results
	reports {
		junitXml.required.set(true)
		html.required.set(true)
	}
}

// ─── JaCoCo coverage ──────────────────────────────────────────────────────────
tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)   // for CI coverage tools
		html.required.set(true)  // for human-readable report artifact
	}
	// Exclude boilerplate: DTOs, JPA entities, generated config, and the boot entry-point
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				exclude(
					"**/dto/**",
					"**/domain/entity/**",
					"**/infrastructure/persistence/entity/**",
					"**/*Application.class",
					"**/*Configuration.class",
					"**/*Properties.class"
				)
			}
		})
	)
}

tasks.named<Test>("test") {
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestCoverageVerification {
	violationRules {
		// Service layer: raised from 60% → 75% (Phase 6 target: 80%)
		rule {
			element = "PACKAGE"
			includes = listOf("com.ziyara.backend.application.service.*")
			limit {
				counter = "LINE"
				value   = "COVEREDRATIO"
				minimum = "0.75".toBigDecimal()
			}
		}
		// Controller layer: must meet 60% aggregate line coverage
		rule {
			element = "PACKAGE"
			includes = listOf("com.ziyara.backend.presentation.controller.*")
			limit {
				counter = "LINE"
				value   = "COVEREDRATIO"
				minimum = "0.60".toBigDecimal()
			}
		}
		// Persistence adapters: must meet 60% aggregate line coverage
		rule {
			element = "PACKAGE"
			includes = listOf("com.ziyara.backend.infrastructure.persistence.adapter.*")
			limit {
				counter = "LINE"
				value   = "COVEREDRATIO"
				minimum = "0.60".toBigDecimal()
			}
		}
		// Critical paths: individual class gates at 80%
		rule {
			element = "CLASS"
			includes = listOf(
				"com.ziyara.backend.application.service.AuthService",
				"com.ziyara.backend.application.service.BookingService"
			)
			limit {
				counter = "LINE"
				value   = "COVEREDRATIO"
				minimum = "0.80".toBigDecimal()
			}
		}
	}
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }

springBoot {
	mainClass.set("com.ziyara.backend.ZiyarahApplication")
}

// ─── AOT (JVM mode) ───────────────────────────────────────────────────────────
// processAot starts a forked Spring context to discover beans. External services
// are replaced with in-process stubs so the build succeeds without Docker.
// Runtime activation: -Dspring.aot.enabled=true is set in Dockerfile JAVA_OPTS.
tasks.withType<org.springframework.boot.gradle.tasks.aot.ProcessAot>().configureEach {
	environment(mapOf(
		// No prod profile — skips PiiEncryptionStartupGuard (@Profile("prod"))
		"SPRING_PROFILES_ACTIVE"                  to "default",
		// JwtService @PostConstruct requires a non-blank secret of ≥32 bytes
		"JWT_SECRET"                              to "aot-build-placeholder-not-used-at-runtime!!",
		// PiiCryptoService optional key (empty default, but provide a valid base64 for safety)
		"ZIYARA_PII_ENCRYPTION_KEY_BASE64"        to "dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3Q=",
		// SuperAdminSeeder / DemoDataSeeder need this; ApplicationRunners run after context
		// refresh, not during processAot, so any non-blank value works
		"APP_DEMO_PASSWORD"                       to "AotBuild1!",
		// Replace PostgreSQL/PgBouncer with H2 in-process — no network needed
		"SPRING_DATASOURCE_URL"                   to "jdbc:h2:mem:aotdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"SPRING_DATASOURCE_DRIVER_CLASS_NAME"     to "org.h2.Driver",
		"SPRING_DATASOURCE_USERNAME"              to "sa",
		"SPRING_DATASOURCE_PASSWORD"              to "",
		"SPRING_JPA_DATABASE_PLATFORM"            to "org.hibernate.dialect.H2Dialect",
		// Skip Flyway — H2 has no migrations; Hibernate create-drop builds the schema instead
		"SPRING_FLYWAY_ENABLED"                   to "false",
		"SPRING_JPA_HIBERNATE_DDL_AUTO"           to "create-drop",
		// Disable Kafka consumer/producer beans — nothing to connect to in a Docker build layer
		"ZIYARA_NOTIFICATIONS_KAFKA_ENABLED"      to "false",
		// Redis connection factory starts lazily enough not to fail, but disable rate-limit
		// Redis to avoid any eager connection attempt in the auth filter chain
		"ZIYARA_RATE_LIMIT_LOGIN_REDIS_ENABLED"   to "false",
		// Bucket4j aspect casts LettuceConnectionFactory → RedisClient in @PostConstruct;
		// that fails in the H2-backed AOT build context — disable the bean entirely
		"APP_RATE_LIMIT_ENABLED"                  to "false",
		// RLS requires PostgreSQL session vars — disable so H2 DataSource is used plainly
		"ZIYARA_RLS_ENABLED"                      to "false",
		// Suppress CORS validation that requires at least one allowed origin
		"ZIYARA_CORS_ALLOW_ALL"                   to "true"
	))
}

// Wire AOT processing into the normal bootJar lifecycle.
// processAot runs before bootJar so the generated bean-factory classes are
// compiled and included in the fat jar. The runtime flag in JAVA_OPTS
// (-Dspring.aot.enabled=true) tells Spring to use them instead of rediscovering
// beans via reflection on every startup.
tasks.named("bootJar") {
	dependsOn("processAot")
}