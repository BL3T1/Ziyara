package com.ziyara.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * @deprecated Superseded by {@link CleanArchitectureDddTest} which covers all rules in this
 *             class and many more. This class is kept only so its five more-granular tests
 *             continue to run as a double-check until CleanArchitectureDddTest has been
 *             validated in CI.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DddLayeringArchitectureTest {

    // -----------------------------------------------------------------------
    // !! IMPORTANT: Change these to match YOUR actual package structure !!
    // Run: find src/main/java -type d | sort   to verify folder names
    // -----------------------------------------------------------------------
    private static final String BASE_PACKAGE = "com.ziyara.backend";

    private static final String DOMAIN_LAYER         = "Domain";
    private static final String APPLICATION_LAYER    = "Application";
    private static final String INFRASTRUCTURE_LAYER = "Infrastructure";
    private static final String INTERFACES_LAYER     = "Interfaces";

    // The ".." suffix means "this package AND all sub-packages" in ArchUnit
    private static final String DOMAIN_PKG         = BASE_PACKAGE + ".domain..";
    private static final String APPLICATION_PKG    = BASE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE_PKG = BASE_PACKAGE + ".infrastructure..";
    private static final String INTERFACES_PKG     = BASE_PACKAGE + ".presentation..";

    // Cross-cutting infrastructure sub-packages that controllers / application services
    // legitimately consume as "outbound ports" or "adapters" (JWT, crypto, cookies, messaging).
    // The critical rule we enforce is that NO code in application or presentation touches
    // the JPA persistence layer directly — that must go through domain repository interfaces.
    private static final String INF_SECURITY_PKG  = BASE_PACKAGE + ".infrastructure.security..";
    private static final String INF_CONFIG_PKG    = BASE_PACKAGE + ".infrastructure.config..";
    private static final String INF_WEB_PKG       = BASE_PACKAGE + ".infrastructure.web..";
    private static final String INF_MESSAGING_PKG = BASE_PACKAGE + ".infrastructure.messaging..";
    private static final String INF_MEDIA_PKG     = BASE_PACKAGE + ".infrastructure.media..";
    // persistence sub-packages that are NOT the forbidden JPA-repo / JPA-entity packages
    private static final String INF_PERSIST_JSON_PKG   = BASE_PACKAGE + ".infrastructure.persistence.json..";
    private static final String INF_PERSIST_MAPPER_PKG = BASE_PACKAGE + ".infrastructure.persistence.mapper..";
    private JavaClasses importedClasses;

    @BeforeAll
    void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    // -----------------------------------------------------------------------
    // 1. Core layered architecture rule
    //
    //    NOTE: consideringAllDependencies() is intentionally NOT used here.
    //    That mode counts bytecode-level synthetic dependencies such as
    //    "Class X extends java.lang.Object" or "annotated with @lombok.Generated"
    //    against the layer rules, producing thousands of false-positive violations
    //    from external libraries that are not in any defined layer.
    //    The default mode only enforces dependencies BETWEEN the four defined layers.
    //
    //    Known cross-cutting infrastructure dependencies (JWT, crypto, cookies,
    //    messaging) are explicitly excluded below via ignoreDependency because they
    //    act as "outbound port adapters" and cannot be moved behind a domain interface
    //    without a much larger refactoring.  The rule's primary goal is to guarantee
    //    that application services and controllers never bypass domain repositories
    //    by importing JPA entities or JPA repositories directly.
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("DDD layered architecture dependencies are respected")
    void dddLayeredArchitectureShouldBeRespected() {
        Architectures.LayeredArchitecture rule = layeredArchitecture()
                // consideringOnlyDependenciesInLayers(): only enforce rules for dependencies
                // BETWEEN the four defined layers.  External libraries (Spring, Lombok, jOOQ, etc.)
                // are not in any layer and are therefore invisible to the rule — no false positives.
                .consideringOnlyDependenciesInLayers()

                .layer(DOMAIN_LAYER)        .definedBy(DOMAIN_PKG)
                .layer(APPLICATION_LAYER)   .definedBy(APPLICATION_PKG)
                .layer(INFRASTRUCTURE_LAYER).definedBy(INFRASTRUCTURE_PKG)
                .layer(INTERFACES_LAYER)    .definedBy(INTERFACES_PKG)

                // Prevents "Layer X is empty" violations for layers not yet populated
                .withOptionalLayers(true)

                .whereLayer(DOMAIN_LAYER).mayNotAccessAnyLayer()
                .whereLayer(APPLICATION_LAYER).mayOnlyAccessLayers(DOMAIN_LAYER)
                .whereLayer(INFRASTRUCTURE_LAYER).mayOnlyAccessLayers(APPLICATION_LAYER, DOMAIN_LAYER)
                .whereLayer(INTERFACES_LAYER).mayOnlyAccessLayers(APPLICATION_LAYER, DOMAIN_LAYER)

                // --- Cross-cutting infrastructure acknowledged as accepted technical debt ---
                // Security / JWT / TOTP / PII crypto: application services (AuthService etc.)
                // and presentation controllers extract userId from JWT tokens.
                .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INF_SECURITY_PKG))
                .ignoreDependency(resideInAPackage(INTERFACES_PKG),  resideInAPackage(INF_SECURITY_PKG))
                // Config properties: AuthController reads JWT-cookie config;
                // PayWebhookController reads payment-gateway config.
                .ignoreDependency(resideInAPackage(INTERFACES_PKG), resideInAPackage(INF_CONFIG_PKG))
                .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INF_CONFIG_PKG))
                // Web helpers: AuthController uses the cookie-helper utility.
                .ignoreDependency(resideInAPackage(INTERFACES_PKG), resideInAPackage(INF_WEB_PKG))
                // Messaging: BookingController publishes staff-notification events directly.
                .ignoreDependency(resideInAPackage(INTERFACES_PKG), resideInAPackage(INF_MESSAGING_PKG))
                .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INF_MESSAGING_PKG))
                // Media storage: image-upload services (HotelRoomService, RestaurantMenuService,
                // ServiceImageService) call MediaStorageService to persist uploaded bytes.
                .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INF_MEDIA_PKG))
                // Web request context: AuditLogService reads the per-request audit holder.
                .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INF_WEB_PKG))
                // Persistence JSON utilities: jOOQ query handlers use UuidListJson for result mapping.
                .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INF_PERSIST_JSON_PKG))
                // Persistence mappers: NotificationService uses NotificationMapper (pre-existing debt).
                .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INF_PERSIST_MAPPER_PKG))
                // domain.payment.PaymentProvider is a hexagonal outbound port whose method signatures
                // reference application-layer payment command/response DTOs.
                .ignoreDependency(
                    resideInAPackage(BASE_PACKAGE + ".domain.payment.."),
                    resideInAPackage(BASE_PACKAGE + ".application.dto.payment.."))

                .as("DDD layered architecture");

        rule.check(importedClasses);
    }

    // -----------------------------------------------------------------------
    // 2. Domain must not depend on Spring Framework
    //    domain.repository interfaces are excluded because they declare
    //    paginated query methods using org.springframework.data.domain.Page /
    //    Pageable as a pragmatic port contract — these types are framework-agnostic
    //    in intent (they define "return a page of results") even though the class
    //    comes from spring-data-commons.
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Domain layer must not depend on Spring Framework")
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN_PKG)
                // domain.repository interfaces may reference Spring Data pagination types
                .and().resideOutsideOfPackage(BASE_PACKAGE + ".domain.repository..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "org.springframework.boot.."
                )
                .as("Domain layer must be framework-agnostic (no Spring dependencies; " +
                    "domain.repository may use Spring Data Page/Pageable as a pagination port contract)")
                .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    // -----------------------------------------------------------------------
    // 3. Domain must not depend on Jakarta EE / JPA / Hibernate
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Domain layer must not depend on Jakarta EE or JPA annotations")
    void domainMustNotDependOnJakartaOrJpa() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN_PKG)
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "jakarta..",
                        "javax..",
                        "org.hibernate.."
                )
                .as("Domain layer must not use Jakarta EE, JPA, or Hibernate")
                .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    // -----------------------------------------------------------------------
    // 4. Domain must not depend on external libraries
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Domain layer must not depend on external libraries")
    void domainMustNotDependOnExternalLibraries() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN_PKG)
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.fasterxml.jackson..",
                        "lombok..",
                        "org.mapstruct..",
                        "io.swagger..",
                        "org.apache..",
                        "com.google.guava..",
                        "reactor.."
                )
                .as("Domain layer must not depend on external libraries")
                .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    // -----------------------------------------------------------------------
    // 5. Domain must not reference any outer layer class
    //    domain.payment is excluded: PaymentProvider is a hexagonal outbound port
    //    whose method signatures reference application-layer command/response DTOs.
    //    This is an accepted design coupling for the payment port definition.
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Domain must not access Application, Infrastructure, or Interfaces")
    void domainMustNotAccessOuterLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN_PKG)
                // domain.payment.PaymentProvider is a port whose signatures use application DTOs
                .and().resideOutsideOfPackage(BASE_PACKAGE + ".domain.payment..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        APPLICATION_PKG,
                        INFRASTRUCTURE_PKG,
                        INTERFACES_PKG
                )
                .as("Domain layer must be completely isolated from outer layers")
                .allowEmptyShould(true);

        rule.check(importedClasses);
    }

    // -----------------------------------------------------------------------
    // 6. Interfaces must not bypass Application and call JPA Infrastructure directly.
    //    The rule is scoped to infrastructure.persistence (JPA repositories and
    //    entities) — the violation group that was systematically fixed.
    //    Cross-cutting infrastructure (security, config, web, messaging) is
    //    intentionally excluded; those are handled by test 1's ignoreDependency
    //    clauses and represent known accepted debt.
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Interfaces layer must not directly depend on JPA persistence infrastructure")
    void interfacesMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(INTERFACES_PKG)
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".infrastructure.persistence..")
                .as("Interfaces layer must not directly access JPA repositories or entities")
                .allowEmptyShould(true);

        rule.check(importedClasses);
    }
}
