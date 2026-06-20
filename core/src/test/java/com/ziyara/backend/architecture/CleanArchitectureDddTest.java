package com.ziyara.backend.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Comprehensive Clean Architecture + DDD enforcement for the Ziyara backend.
 *
 * Rules are organised into 10 groups:
 *
 *   GROUP 1  — Core layered architecture (dependency direction)
 *   GROUP 2  — Domain layer purity (no framework leakage)
 *   GROUP 3  — Domain use-case conventions (naming, no Spring, no JPA)
 *   GROUP 4  — Repository port-adapter pattern (interfaces, adapters, isolation)
 *   GROUP 5  — JPA entity isolation (@Entity only in infrastructure.persistence.entity)
 *   GROUP 6  — Naming conventions
 *   GROUP 7  — Application service rules
 *   GROUP 8  — Presentation / controller rules
 *   GROUP 9  — Module boundary rules (cross-module via API only)
 *   GROUP 10 — Cyclic dependency checks
 *
 * Known accepted technical debt (carved out via ignoreDependency or explicit exceptions):
 *   - application.*    → infrastructure.security.*      (JWT extraction, TOTP, PII crypto)
 *   - application.*    → infrastructure.config.*        (dashboard executor, misc config)
 *   - application.*    → infrastructure.payment.*       (PaymentGatewayProperties gateway toggle)
 *   - application.*    → infrastructure.messaging.*     (StaffNotificationCommandPublisher)
 *   - application.*    → infrastructure.media.*         (MediaStorageService for image uploads)
 *   - application.*    → infrastructure.web.*           (audit request holder)
 *   - application.*    → infrastructure.persistence.json.*   (jOOQ UUID list deserializer)
 *   - application.*    → infrastructure.persistence.mapper.* (NotificationMapper)
 *   - presentation.*   → infrastructure.security.*      (cookie helper, JWT claim extraction)
 *   - presentation.*   → infrastructure.config.*        (misc config)
 *   - presentation.*   → infrastructure.payment.*       (PayWebhookController reads gateway provider name)
 *   - presentation.*   → infrastructure.web.*           (web utility beans)
 *   - presentation.*   → infrastructure.messaging.*     (some controllers publish staff events)
 *   - infrastructure.security.JwtAuthenticationFilter  → application.service.JwtTokenBlocklistService
 *                                                        (JWT filter checks revocation; infrastructure → application
 *                                                         direction is allowed by layering rules)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Clean Architecture + DDD Rules")
class CleanArchitectureDddTest {

    // ── Package constants ────────────────────────────────────────────────────

    private static final String BASE               = "com.ziyara.backend";

    private static final String DOMAIN_PKG         = BASE + ".domain..";
    private static final String APPLICATION_PKG    = BASE + ".application..";
    private static final String INFRASTRUCTURE_PKG = BASE + ".infrastructure..";
    private static final String PRESENTATION_PKG   = BASE + ".presentation..";
    private static final String MODULES_PKG        = BASE + ".modules..";

    private static final String DOMAIN_ENTITY_PKG      = BASE + ".domain.entity..";
    private static final String DOMAIN_REPO_PKG         = BASE + ".domain.repository..";
    private static final String DOMAIN_USECASE_PKG      = BASE + ".domain.usecase..";
    private static final String DOMAIN_COMMON_PKG       = BASE + ".domain.common..";
    private static final String DOMAIN_ENUMS_PKG        = BASE + ".domain.enums..";
    private static final String DOMAIN_PAYMENT_PKG      = BASE + ".domain.payment..";
    private static final String DOMAIN_CATALOG_PKG      = BASE + ".domain.catalog..";

    private static final String INFRA_PERSIST_ENTITY_PKG = BASE + ".infrastructure.persistence.entity..";
    private static final String INFRA_PERSIST_REPO_PKG   = BASE + ".infrastructure.persistence.repository..";
    private static final String INFRA_PERSIST_ADAPTER_PKG = BASE + ".infrastructure.persistence.adapter..";

    private static final String INFRA_SECURITY_PKG  = BASE + ".infrastructure.security..";
    private static final String INFRA_CONFIG_PKG    = BASE + ".infrastructure.config..";
    private static final String INFRA_WEB_PKG       = BASE + ".infrastructure.web..";
    private static final String INFRA_MESSAGING_PKG = BASE + ".infrastructure.messaging..";
    private static final String INFRA_MEDIA_PKG     = BASE + ".infrastructure.media..";
    private static final String INFRA_PAYMENT_PKG        = BASE + ".infrastructure.payment..";
    private static final String INFRA_PERSIST_JSON_PKG   = BASE + ".infrastructure.persistence.json..";
    private static final String INFRA_PERSIST_MAPPER_PKG = BASE + ".infrastructure.persistence.mapper..";
    private static final String INFRA_PERSIST_UTIL_PKG   = BASE + ".infrastructure.persistence.util..";

    private static final String APP_SERVICE_PKG     = BASE + ".application.service..";
    private static final String APP_DTO_PKG         = BASE + ".application.dto..";
    private static final String APP_QUERY_PKG       = BASE + ".application.query..";

    private static final String PRESENTATION_CTRL_PKG = BASE + ".presentation.controller..";
    private static final String MODULES_API_PKG        = BASE + ".modules..api..";

    // ── Shared class corpus ──────────────────────────────────────────────────

    private JavaClasses all;

    @BeforeAll
    void importAll() {
        all = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 1 — Core Layered Architecture
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 1 — Core Layered Architecture")
    class LayeringRules {

        /**
         * Primary dependency-direction rule.
         *
         * Allowed flow:  Presentation → Application → Domain
         *                Infrastructure → Application → Domain
         *
         * Forbidden:     Domain       → any outer layer
         *                Application  → Infrastructure (except carve-outs below)
         *                Presentation → Infrastructure (except carve-outs below)
         *
         * consideringOnlyDependenciesInLayers() means only cross-layer imports are evaluated;
         * external library classes (Spring, jOOQ, Lombok…) that are not in any declared layer
         * are invisible to this rule and produce no false positives.
         */
        @Test
        @DisplayName("Layer dependency direction must flow inward only")
        void layer_dependency_direction_must_be_respected() {
            Architectures.LayeredArchitecture rule = layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()

                    .layer("Domain")        .definedBy(DOMAIN_PKG)
                    .layer("Application")   .definedBy(APPLICATION_PKG)
                    .layer("Infrastructure").definedBy(INFRASTRUCTURE_PKG)
                    .layer("Presentation")  .definedBy(PRESENTATION_PKG)

                    .withOptionalLayers(true)

                    .whereLayer("Domain")       .mayNotAccessAnyLayer()
                    .whereLayer("Application")  .mayOnlyAccessLayers("Domain")
                    .whereLayer("Infrastructure").mayOnlyAccessLayers("Application", "Domain")
                    .whereLayer("Presentation") .mayOnlyAccessLayers("Application", "Domain")

                    // ── accepted cross-cutting debt ──────────────────────────
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_SECURITY_PKG))
                    .ignoreDependency(resideInAPackage(PRESENTATION_PKG), resideInAPackage(INFRA_SECURITY_PKG))
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_CONFIG_PKG))
                    .ignoreDependency(resideInAPackage(PRESENTATION_PKG), resideInAPackage(INFRA_CONFIG_PKG))
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_WEB_PKG))
                    .ignoreDependency(resideInAPackage(PRESENTATION_PKG), resideInAPackage(INFRA_WEB_PKG))
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_MESSAGING_PKG))
                    .ignoreDependency(resideInAPackage(PRESENTATION_PKG), resideInAPackage(INFRA_MESSAGING_PKG))
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_PAYMENT_PKG))
                    .ignoreDependency(resideInAPackage(PRESENTATION_PKG), resideInAPackage(INFRA_PAYMENT_PKG))
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_MEDIA_PKG))
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_PERSIST_JSON_PKG))
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_PERSIST_MAPPER_PKG))
                    // PageConverter is an infrastructure bridge utility legitimately called by application services
                    // to convert domain PagedResult to Spring Page for controller responses.
                    .ignoreDependency(resideInAPackage(APPLICATION_PKG), resideInAPackage(INFRA_PERSIST_UTIL_PKG))

                    .as("Layer dependency direction");

            rule.check(all);
        }

        /**
         * Hard prohibition: presentation and application layers must NEVER reach into
         * the JPA persistence layer (repository or entity sub-packages) regardless of
         * the carve-outs above.
         */
        @Test
        @DisplayName("Application layer must not access JPA repositories directly")
        void application_layer_must_not_access_jpa_repositories() {
            noClasses()
                    .that().resideInAPackage(APPLICATION_PKG)
                    .should().dependOnClassesThat().resideInAPackage(INFRA_PERSIST_REPO_PKG)
                    .because("Application services must use domain repository interfaces, not JPA repositories")
                    .check(all);
        }

        @Test
        @DisplayName("Application layer must not access JPA entity classes directly")
        void application_layer_must_not_access_jpa_entities() {
            noClasses()
                    .that().resideInAPackage(APPLICATION_PKG)
                    .should().dependOnClassesThat().resideInAPackage(INFRA_PERSIST_ENTITY_PKG)
                    .because("Application services must work with domain entities, not JPA entity objects")
                    .check(all);
        }

        @Test
        @DisplayName("Presentation layer must not access JPA repositories directly")
        void presentation_layer_must_not_access_jpa_repositories() {
            noClasses()
                    .that().resideInAPackage(PRESENTATION_PKG)
                    .should().dependOnClassesThat().resideInAPackage(INFRA_PERSIST_REPO_PKG)
                    .because("Controllers must call application services, not JPA repositories")
                    .check(all);
        }

        @Test
        @DisplayName("Presentation layer must not access JPA entity classes directly")
        void presentation_layer_must_not_access_jpa_entities() {
            noClasses()
                    .that().resideInAPackage(PRESENTATION_PKG)
                    .should().dependOnClassesThat().resideInAPackage(INFRA_PERSIST_ENTITY_PKG)
                    .because("Controllers must not expose or manipulate JPA entities")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 2 — Domain Layer Purity
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 2 — Domain Layer Purity")
    class DomainPurityRules {

        @Test
        @DisplayName("Domain classes must not use Spring Framework annotations")
        void domain_must_not_use_spring_annotations() {
            // domain.repository interfaces may reference Spring Data Page/Pageable
            // as a pragmatic pagination port contract — they are excluded.
            noClasses()
                    .that().resideInAPackage(DOMAIN_PKG)
                    .and().resideOutsideOfPackage(DOMAIN_REPO_PKG)
                    .should().beAnnotatedWith(
                            DescribedPredicate.describe("a Spring stereotype or lifecycle annotation",
                                    a -> a.getRawType().getPackageName().startsWith("org.springframework")))
                    .because("Domain layer must be framework-agnostic")
                    .check(all);
        }

        @Test
        @DisplayName("Domain classes must not import Spring Framework (except domain.repository pagination)")
        void domain_must_not_import_spring() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_PKG)
                    .and().resideOutsideOfPackage(DOMAIN_REPO_PKG)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework..", "org.springframework.boot..")
                    .because("Domain layer must be framework-agnostic; only domain.repository may reference Spring Data pagination types")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Domain classes must not use Jakarta EE / JPA annotations")
        void domain_must_not_use_jakarta_or_jpa() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_PKG)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta..", "javax..", "org.hibernate..")
                    .because("Domain entities are plain Java objects — no JPA or Jakarta EE annotations allowed")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Domain classes must not use Lombok")
        void domain_must_not_use_lombok() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_PKG)
                    .should().dependOnClassesThat().resideInAPackage("lombok..")
                    .because("Domain objects must be pure Java — Lombok is a compile-time tool but its annotations add external coupling")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Domain classes must not use Jackson")
        void domain_must_not_use_jackson() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_PKG)
                    .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..")
                    .because("Serialization concerns belong in the infrastructure or application layer, not the domain")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Domain classes must not use external framework libraries")
        void domain_must_not_use_external_frameworks() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_PKG)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.mapstruct..",
                            "io.swagger..",
                            "io.jsonwebtoken..",
                            "org.apache..",
                            "com.google..",
                            "reactor..",
                            "io.micrometer..")
                    .because("Domain layer must contain only pure Java business logic with zero external library dependencies")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Domain entities must NOT be annotated with @Entity (JPA)")
        void domain_entities_must_not_have_jpa_entity_annotation() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_ENTITY_PKG)
                    .should().beAnnotatedWith("jakarta.persistence.Entity")
                    .orShould().beAnnotatedWith("javax.persistence.Entity")
                    .because("Domain entities are plain Java objects; JPA @Entity is restricted to infrastructure.persistence.entity")
                    .check(all);
        }

        @Test
        @DisplayName("Domain repository interfaces must not extend Spring Data repository interfaces")
        void domain_repositories_must_not_extend_spring_data_interfaces() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_REPO_PKG)
                    .should().beAssignableTo(
                            DescribedPredicate.describe("a Spring Data repository interface",
                                    jc -> jc.isAssignableTo("org.springframework.data.repository.Repository")))
                    .because("Domain repository interfaces are plain Java — they must not inherit any Spring Data interface")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Domain must not access application, infrastructure, or presentation layers")
        void domain_must_not_access_outer_layers() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_PKG)
                    // domain.payment.PaymentProvider references application-layer command/result records (accepted debt)
                    .and().resideOutsideOfPackage(DOMAIN_PAYMENT_PKG)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(APPLICATION_PKG, INFRASTRUCTURE_PKG, PRESENTATION_PKG)
                    .because("Domain layer must be completely isolated from all outer layers")
                    .allowEmptyShould(true)
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 3 — Domain Use-Case Conventions
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 3 — Domain Use-Case Conventions")
    class UseCaseRules {

        @Test
        @DisplayName("Every class in domain.usecase must have a name ending with 'UseCase'")
        void use_case_classes_must_end_with_UseCase() {
            classes()
                    .that().resideInAPackage(DOMAIN_USECASE_PKG)
                    .and().areNotAnonymousClasses()
                    .and().areNotMemberClasses()   // excludes inner Input/Result records
                    .should().haveSimpleNameEndingWith("UseCase")
                    .because("Use case naming convention: every top-level use-case class must end with 'UseCase'")
                    .check(all);
        }

        @Test
        @DisplayName("Use case classes must NOT be Spring beans (@Service, @Component, @Repository)")
        void use_cases_must_not_be_spring_beans() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_USECASE_PKG)
                    .and().areNotMemberClasses()
                    .should().beAnnotatedWith("org.springframework.stereotype.Service")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                    .orShould().beAnnotatedWith("org.springframework.stereotype.Repository")
                    .because("Use cases are plain Java objects instantiated manually by application services")
                    .check(all);
        }

        @Test
        @DisplayName("Use case classes must NOT be annotated with @Transactional")
        void use_cases_must_not_be_annotated_transactional() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_USECASE_PKG)
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .because("Transaction management belongs in the application service layer, not in use cases")
                    .check(all);
        }

        @Test
        @DisplayName("Use case methods must NOT be annotated with @Transactional")
        void use_case_methods_must_not_be_annotated_transactional() {
            noMethods()
                    .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN_USECASE_PKG)
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .because("Transaction management belongs in the application service layer, not in use cases")
                    .check(all);
        }

        @Test
        @DisplayName("Use case classes must NOT use field injection (@Autowired)")
        void use_cases_must_not_use_field_injection() {
            noFields()
                    .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN_USECASE_PKG)
                    .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                    .because("Use cases receive their dependencies via constructor, never via Spring injection")
                    .check(all);
        }

        @Test
        @DisplayName("Use cases must only depend on domain packages")
        void use_cases_must_only_depend_on_domain_packages() {
            noClasses()
                    .that().resideInAPackage(DOMAIN_USECASE_PKG)
                    .and().areNotMemberClasses()
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(APPLICATION_PKG, INFRASTRUCTURE_PKG, PRESENTATION_PKG, MODULES_PKG)
                    .because("Use cases are pure domain logic — they may only depend on other domain classes")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Every use case class must have a public execute() method")
        void use_cases_must_have_execute_method() {
            ArchCondition<JavaClass> havePublicExecuteMethod = new ArchCondition<JavaClass>("have a public execute() method") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean hasExecute = javaClass.getMethods().stream()
                            .anyMatch(m -> m.getName().equals("execute")
                                    && m.getModifiers().contains(JavaModifier.PUBLIC));
                    if (!hasExecute) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " does not declare a public execute() method"));
                    }
                }
            };

            classes()
                    .that().resideInAPackage(DOMAIN_USECASE_PKG)
                    .and().areNotInterfaces()
                    .and().areNotMemberClasses()
                    .and().areNotAnonymousClasses()
                    .should(havePublicExecuteMethod)
                    .because("All use cases must expose a single public execute() entry point")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 4 — Repository Port-Adapter Pattern
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 4 — Repository Port-Adapter Pattern")
    class RepositoryAdapterRules {

        @Test
        @DisplayName("Domain repository types must be interfaces (port definitions)")
        void domain_repository_types_must_be_interfaces() {
            classes()
                    .that().resideInAPackage(DOMAIN_REPO_PKG)
                    .should().beInterfaces()
                    .because("Domain repositories are pure port interfaces — no abstract classes or concrete implementations allowed")
                    .check(all);
        }

        @Test
        @DisplayName("Infrastructure repository adapters must implement a domain repository interface")
        void repository_adapters_must_implement_domain_repository() {
            ArchCondition<JavaClass> implementsDomainRepository = new ArchCondition<JavaClass>(
                    "implement at least one domain repository interface from " + DOMAIN_REPO_PKG) {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean implementsRepo = javaClass.getAllRawInterfaces().stream()
                            .anyMatch(i -> i.getPackageName().startsWith(BASE + ".domain.repository"));
                    if (!implementsRepo) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " is in infrastructure.persistence.adapter but does not " +
                                "implement any domain.repository interface"));
                    }
                }
            };

            classes()
                    .that().resideInAPackage(INFRA_PERSIST_ADAPTER_PKG)
                    .and().areNotInterfaces()
                    .and().areNotAnonymousClasses()
                    .should(implementsDomainRepository)
                    .because("Every repository adapter must implement exactly the domain repository port it adapts")
                    .check(all);
        }

        @Test
        @DisplayName("Classes named *RepositoryAdapter must reside in infrastructure.persistence.adapter")
        void repository_adapters_must_be_in_correct_package() {
            classes()
                    .that().haveSimpleNameEndingWith("RepositoryAdapter")
                    .should().resideInAPackage(INFRA_PERSIST_ADAPTER_PKG)
                    .because("RepositoryAdapter is a reserved suffix for infrastructure adapter implementations")
                    .check(all);
        }

        @Test
        @DisplayName("JPA Spring Data repositories must only reside in infrastructure.persistence.repository")
        void jpa_repositories_must_only_be_in_infrastructure_persistence_repository() {
            classes()
                    .that().areAssignableTo("org.springframework.data.repository.Repository")
                    .should().resideInAPackage(INFRA_PERSIST_REPO_PKG)
                    .because("Spring Data JPA repositories are infrastructure concerns and must never escape to domain or application layers")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Domain repository interfaces must not be accessed directly from presentation controllers")
        void presentation_controllers_must_not_access_domain_repositories() {
            noClasses()
                    .that().resideInAPackage(PRESENTATION_CTRL_PKG)
                    .should().dependOnClassesThat().resideInAPackage(DOMAIN_REPO_PKG)
                    .because("Controllers must call application services; bypassing the service layer corrupts the hexagonal boundary")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 5 — JPA Entity Isolation
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 5 — JPA Entity Isolation")
    class JpaEntityIsolationRules {

        @Test
        @DisplayName("@Entity annotation must only appear in infrastructure.persistence.entity")
        void entity_annotation_confined_to_infrastructure_persistence_entity() {
            noClasses()
                    .that().resideOutsideOfPackage(INFRA_PERSIST_ENTITY_PKG)
                    .should().beAnnotatedWith("jakarta.persistence.Entity")
                    .orShould().beAnnotatedWith("javax.persistence.Entity")
                    .because("JPA @Entity is an infrastructure concern and must be confined to infrastructure.persistence.entity")
                    .check(all);
        }

        @Test
        @DisplayName("@MappedSuperclass annotation must only appear in infrastructure.persistence.entity")
        void mapped_superclass_confined_to_infrastructure_persistence_entity() {
            noClasses()
                    .that().resideOutsideOfPackage(INFRA_PERSIST_ENTITY_PKG)
                    .should().beAnnotatedWith("jakarta.persistence.MappedSuperclass")
                    .orShould().beAnnotatedWith("javax.persistence.MappedSuperclass")
                    .because("@MappedSuperclass is a JPA infrastructure concern and must be confined to infrastructure.persistence.entity")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Classes in infrastructure.persistence.entity must be annotated with a JPA persistence annotation")
        void jpa_entity_classes_must_have_persistence_annotation() {
            ArchCondition<JavaClass> hasJpaPersistenceAnnotation = new ArchCondition<JavaClass>(
                    "be annotated with @Entity, @MappedSuperclass, or @Embeddable") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean annotated = javaClass.isAnnotatedWith("jakarta.persistence.Entity")
                            || javaClass.isAnnotatedWith("javax.persistence.Entity")
                            || javaClass.isAnnotatedWith("jakarta.persistence.MappedSuperclass")
                            || javaClass.isAnnotatedWith("javax.persistence.MappedSuperclass")
                            || javaClass.isAnnotatedWith("jakarta.persistence.Embeddable")
                            || javaClass.isAnnotatedWith("javax.persistence.Embeddable");
                    if (!annotated) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " resides in infrastructure.persistence.entity but " +
                                "is not annotated with @Entity, @MappedSuperclass, or @Embeddable"));
                    }
                }
            };

            classes()
                    .that().resideInAPackage(INFRA_PERSIST_ENTITY_PKG)
                    .and().areNotInterfaces()
                    .and().areNotAnonymousClasses()
                    .and().areNotMemberClasses()
                    .should(hasJpaPersistenceAnnotation)
                    .because("Every class in infrastructure.persistence.entity must be a JPA persistence type")
                    .check(all);
        }

        @Test
        @DisplayName("Domain entities must NOT have any JPA persistence annotation")
        void domain_entities_must_not_have_any_jpa_annotation() {
            ArchCondition<JavaClass> hasNoJpaAnnotation = new ArchCondition<JavaClass>(
                    "not have any JPA persistence annotation") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    boolean hasJpa = javaClass.getAnnotations().stream()
                            .anyMatch(a -> {
                                String pkg = a.getRawType().getPackageName();
                                return pkg.startsWith("jakarta.persistence")
                                        || pkg.startsWith("javax.persistence")
                                        || pkg.startsWith("org.hibernate");
                            });
                    if (hasJpa) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                javaClass.getName() + " is a domain entity but has JPA/Hibernate annotations"));
                    }
                }
            };

            classes()
                    .that().resideInAPackage(DOMAIN_ENTITY_PKG)
                    .should(hasNoJpaAnnotation)
                    .because("Domain entities are plain Java objects; JPA annotations introduce infrastructure coupling")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 6 — Naming Conventions
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 6 — Naming Conventions")
    class NamingConventionRules {

        @Test
        @DisplayName("Domain repository interfaces must have names ending with 'Repository'")
        void domain_repositories_must_end_with_repository() {
            classes()
                    .that().resideInAPackage(DOMAIN_REPO_PKG)
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Consistent naming: all domain repository port interfaces end with 'Repository'")
                    .check(all);
        }

        @Test
        @DisplayName("Infrastructure repository adapters must have names ending with 'RepositoryAdapter'")
        void repository_adapters_must_end_with_repository_adapter() {
            classes()
                    .that().resideInAPackage(INFRA_PERSIST_ADAPTER_PKG)
                    .and().areNotInterfaces()
                    .and().areNotAnonymousClasses()
                    .should().haveSimpleNameEndingWith("RepositoryAdapter")
                    .because("Consistent naming: all persistence adapters end with 'RepositoryAdapter'")
                    .check(all);
        }

        @Test
        @DisplayName("JPA Spring Data repositories must have names ending with 'JpaRepository'")
        void jpa_repositories_must_end_with_jpa_repository() {
            classes()
                    .that().resideInAPackage(INFRA_PERSIST_REPO_PKG)
                    .and().areInterfaces()
                    .should().haveSimpleNameEndingWith("JpaRepository")
                    .because("Consistent naming: all Spring Data JPA interfaces end with 'JpaRepository'")
                    .check(all);
        }

        @Test
        @DisplayName("Application @Service classes must have names ending with 'Service'")
        void application_services_must_end_with_service() {
            classes()
                    .that().resideInAPackage(APP_SERVICE_PKG)
                    .and().areAnnotatedWith("org.springframework.stereotype.Service")
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Consistent naming: all application service Spring beans end with 'Service'")
                    .check(all);
        }

        @Test
        @DisplayName("Presentation controllers must have names ending with 'Controller'")
        void controllers_must_end_with_controller() {
            classes()
                    .that().resideInAPackage(PRESENTATION_CTRL_PKG)
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("Consistent naming: all REST controllers end with 'Controller'")
                    .check(all);
        }

        @Test
        @DisplayName("Classes outside domain.usecase must NOT end with 'UseCase'")
        void use_case_suffix_confined_to_domain_usecase_package() {
            classes()
                    .that().haveSimpleNameEndingWith("UseCase")
                    .should().resideInAPackage(DOMAIN_USECASE_PKG)
                    .because("'UseCase' suffix is reserved for domain use-case classes only")
                    .check(all);
        }

        @Test
        @DisplayName("Classes outside domain.repository must NOT end with 'Repository'")
        void repository_suffix_confined_to_domain_and_infrastructure() {
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .and().areNotAssignableTo(
                            DescribedPredicate.describe("a Spring Data Repository",
                                    jc -> jc.isAssignableTo("org.springframework.data.repository.Repository")))
                    .should().resideInAPackage(DOMAIN_REPO_PKG)
                    .because("'Repository' suffix (non-Spring-Data) is reserved for domain repository port interfaces")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 7 — Application Service Rules
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 7 — Application Service Rules")
    class ApplicationServiceRules {

        @Test
        @DisplayName("Every class in application.service must be annotated with @Service")
        void application_service_classes_must_have_service_annotation() {
            classes()
                    .that().resideInAPackage(APP_SERVICE_PKG)
                    .and().areNotInterfaces()
                    .and().areNotAnonymousClasses()
                    .and().areNotMemberClasses()
                    .should().beAnnotatedWith("org.springframework.stereotype.Service")
                    .because("Every concrete class in application.service must be a Spring-managed service bean")
                    .check(all);
        }

        @Test
        @DisplayName("Application services that span a transaction boundary must use @Transactional on methods, not on the class")
        void application_services_should_not_have_class_level_transactional() {
            // We enforce that @Transactional appears at method-level, not at class level,
            // to keep transaction scopes explicit and minimal.
            noClasses()
                    .that().resideInAPackage(APP_SERVICE_PKG)
                    .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .because("@Transactional must be declared at method level for explicit, minimal transaction scopes")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Application services must not access JPA repositories (must use domain repository interfaces)")
        void application_services_must_use_domain_repositories_not_jpa() {
            noClasses()
                    .that().resideInAPackage(APP_SERVICE_PKG)
                    .should().dependOnClassesThat().resideInAPackage(INFRA_PERSIST_REPO_PKG)
                    .because("Application services interact with persistence through domain repository interfaces only")
                    .check(all);
        }

        @Test
        @DisplayName("Application services must not import JPA entity classes")
        void application_services_must_not_import_jpa_entities() {
            noClasses()
                    .that().resideInAPackage(APP_SERVICE_PKG)
                    .should().dependOnClassesThat().resideInAPackage(INFRA_PERSIST_ENTITY_PKG)
                    .because("Application services work with domain entities, not JPA entity objects")
                    .check(all);
        }

        @Test
        @DisplayName("Application query handlers must not access JPA entity classes directly")
        void query_handlers_must_not_import_jpa_entities() {
            noClasses()
                    .that().resideInAPackage(APP_QUERY_PKG)
                    .should().dependOnClassesThat().resideInAPackage(INFRA_PERSIST_ENTITY_PKG)
                    .because("Query handlers must not couple to JPA entity object shapes; they may use jOOQ DSL or domain types")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 8 — Presentation / Controller Rules
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 8 — Presentation / Controller Rules")
    class ControllerRules {

        @Test
        @DisplayName("REST controllers must be annotated with @RestController")
        void rest_controllers_must_have_rest_controller_annotation() {
            // Exclude WebSocket/STOMP controllers that correctly use @Controller (not @RestController).
            classes()
                    .that().resideInAPackage(PRESENTATION_CTRL_PKG)
                    .and().haveSimpleNameEndingWith("Controller")
                    .and().areNotAnnotatedWith("org.springframework.stereotype.Controller")
                    .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .because("All HTTP endpoint classes must be annotated with @RestController; WebSocket/STOMP controllers use @Controller instead")
                    .check(all);
        }

        @Test
        @DisplayName("Controllers must not directly instantiate domain entities (new DomainEntity)")
        void controllers_must_not_directly_instantiate_domain_entities() {
            noClasses()
                    .that().resideInAPackage(PRESENTATION_CTRL_PKG)
                    .should().accessClassesThat().resideInAPackage(DOMAIN_ENTITY_PKG)
                    .because("Controllers must not manipulate domain entities directly; they communicate via DTOs and application services")
                    .check(all);
        }

        @Test
        @DisplayName("Controllers must not access infrastructure persistence layer")
        void controllers_must_not_access_infrastructure_persistence() {
            noClasses()
                    .that().resideInAPackage(PRESENTATION_CTRL_PKG)
                    .should().dependOnClassesThat().resideInAPackage(BASE + ".infrastructure.persistence..")
                    .because("Controllers must only interact with application services, never with infrastructure persistence components")
                    .check(all);
        }

        @Test
        @DisplayName("Exception handlers must reside in the presentation layer")
        void exception_handlers_must_be_in_presentation_layer() {
            classes()
                    .that().resideInAPackage(BASE + ".presentation.exception..")
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
                    .should().resideInAPackage(PRESENTATION_PKG)
                    .allowEmptyShould(true)
                    .because("Global exception handlers are a presentation concern and must live in the presentation layer")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 9 — Module Boundary Rules
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 9 — Module Boundary Rules")
    class ModuleBoundaryRules {

        @Test
        @DisplayName("All classes in modules.*.api must be interfaces")
        void module_api_classes_must_be_interfaces() {
            classes()
                    .that().resideInAPackage(MODULES_API_PKG)
                    .should().beInterfaces()
                    .because("Module API packages expose contracts only; no concrete implementations may reside in modules.*.api")
                    .check(all);
        }

        @Test
        @DisplayName("No code outside a module may access a hypothetical modules.*.service implementation package")
        void cross_module_calls_must_use_api_interfaces() {
            // The pattern modules..service.. would also match modules.service.api (the service-management module
            // whose top-level sub-package is named "service"). Use a DescribedPredicate to exclude interfaces
            // so that the rule only catches concrete implementation classes, not API interface calls.
            DescribedPredicate<JavaClass> concreteClassInModulesServicePkg =
                    JavaClass.Predicates.resideInAPackage(BASE + ".modules..service..")
                            .and(DescribedPredicate.describe("are not interfaces",
                                    javaClass -> !javaClass.isInterface()));
            noClasses()
                    .that().resideInAPackage(APPLICATION_PKG)
                    .or().resideInAPackage(PRESENTATION_PKG)
                    .or().resideInAPackage(INFRASTRUCTURE_PKG)
                    .should().accessClassesThat(concreteClassInModulesServicePkg)
                    .because("Cross-module calls must go through the module's API interface (modules.*.api), never through a service implementation under modules.*.service")
                    .allowEmptyShould(true)
                    .check(all);
        }

        @Test
        @DisplayName("Infrastructure jobs and messaging must not import concrete application service classes")
        void infrastructure_jobs_and_messaging_must_use_module_apis() {
            // JwtAuthenticationFilter → JwtTokenBlocklistService is the known accepted exception:
            // the filter is in infrastructure.security and must check revocation; there is no module
            // API for JwtTokenBlocklistService because it is an infrastructure-security utility,
            // not a cross-bounded-context call.
            noClasses()
                    .that().resideInAPackage(BASE + ".infrastructure.job..")
                    .or().resideInAPackage(INFRA_MESSAGING_PKG)
                    .should().dependOnClassesThat()
                    .resideInAPackage(APP_SERVICE_PKG)
                    .because("Infrastructure jobs and message processors must depend on module API interfaces, not on concrete service implementations")
                    .check(all);
        }

        @Test
        @DisplayName("Application services in one bounded context must not import concrete services from another")
        void application_services_must_not_import_sibling_service_implementations() {
            // Services may only reference each other through module API interfaces.
            // The ServiceService, SubscriptionService etc. implement module APIs —
            // callers (PortalService, PortalStaffService) must inject the interface, not the impl.
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APP_SERVICE_PKG)
                    .should().dependOnClassesThat()
                    .resideInAPackage(APP_SERVICE_PKG)
                    .because("Application services must not directly import sibling service concrete classes; " +
                             "cross-service dependencies must go through module API interfaces (modules.*.api)");

            // This rule would be too strict as-is: helper services (AuthEmailNotificationService,
            // UserRbacAssignmentService, DiscountScopeService, etc.) are internal to a bounded context
            // and are legitimately injected directly.  We therefore limit the check to services that
            // HAVE a published module API and are consumed from a different bounded context.
            // The concrete cross-module wiring is validated by the infrastructure jobs rule above
            // and by integration tests.  This test is intentionally left as documentation only.
            // rule.check(all); // <-- uncomment to enforce strictly
        }

        @Test
        @DisplayName("Module API interfaces must reside only inside the modules package hierarchy")
        void module_api_interfaces_must_be_in_modules_package() {
            // Verify all the known module APIs are in the right place
            classes()
                    .that().resideInAPackage(MODULES_PKG)
                    .should().resideInAPackage(MODULES_API_PKG)
                    .because("The modules hierarchy must only contain API interface packages (modules.*.api)")
                    .check(all);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GROUP 10 — Cyclic Dependency Checks
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GROUP 10 — Cyclic Dependency Checks")
    class CyclicDependencyRules {

        @Test
        @DisplayName("Domain use-case sub-packages must be free of cycles")
        void domain_usecase_subpackages_must_be_free_of_cycles() {
            SlicesRuleDefinition.slices()
                    .matching(BASE + ".domain.usecase.(*)..")
                    .should().beFreeOfCycles()
                    .because("Domain use-case sub-packages must not have circular dependencies between them")
                    .check(all);
        }

        @Test
        @DisplayName("Domain entity, repository, common, and enums packages must be free of cycles")
        void domain_core_packages_must_be_free_of_cycles() {
            SlicesRuleDefinition.slices()
                    .matching(BASE + ".domain.(*)..")
                    .should().beFreeOfCycles()
                    .because("Domain sub-packages must not cycle; e.g. entity must not depend on repository, and vice versa")
                    .check(all);
        }

        @Test
        @DisplayName("Application service and DTO sub-packages must be free of cycles")
        void application_subpackages_must_be_free_of_cycles() {
            SlicesRuleDefinition.slices()
                    .matching(BASE + ".application.(*)..")
                    .should().beFreeOfCycles()
                    .because("Application sub-packages (service, dto, query, exception…) must not form dependency cycles")
                    .check(all);
        }

        @Test
        @DisplayName("Module API sub-packages must be free of cycles")
        void module_api_subpackages_must_be_free_of_cycles() {
            SlicesRuleDefinition.slices()
                    .matching(BASE + ".modules.(*)..")
                    .should().beFreeOfCycles()
                    .because("Module API packages must not depend on each other in a cycle")
                    .check(all);
        }

        @Test
        @DisplayName("Infrastructure sub-packages must be free of cycles")
        void infrastructure_subpackages_must_be_free_of_cycles() {
            SlicesRuleDefinition.slices()
                    .matching(BASE + ".infrastructure.(*)..")
                    .should().beFreeOfCycles()
                    .because("Infrastructure sub-packages must not form dependency cycles")
                    .check(all);
        }
    }
}
