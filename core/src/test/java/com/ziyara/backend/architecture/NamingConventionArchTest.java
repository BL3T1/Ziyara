package com.ziyara.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Supplemental naming-convention checks that sit outside the 56 rules in
 * {@link CleanArchitectureDddTest}. Each rule is kept narrow so failures are easy to diagnose.
 */
@DisplayName("Naming Convention Architecture Tests")
class NamingConventionArchTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ziyara.backend");
    }

    @Nested
    @DisplayName("Service naming")
    class ServiceNaming {

        @Test
        @DisplayName("Classes ending 'Service' reside in application.service or domain")
        void serviceClassesLiveInCorrectPackage() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Service")
                    .and().areNotInterfaces()
                    .should().resideInAnyPackage(
                            "com.ziyara.backend.application.service..",
                            "com.ziyara.backend.domain..",
                            "com.ziyara.backend.infrastructure..",
                            "com.ziyara.backend.modules.."
                    );
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Controller naming")
    class ControllerNaming {

        @Test
        @DisplayName("Classes ending 'Controller' are annotated @RestController")
        void controllerClassesAreAnnotated() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .and().areNotInterfaces()
                    .should().beAnnotatedWith(RestController.class);
            rule.check(classes);
        }

        @Test
        @DisplayName("Classes annotated @RestController end with 'Controller'")
        void annotatedControllersEndWithController() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Repository naming")
    class RepositoryNaming {

        @Test
        @DisplayName("Classes ending 'Repository' in domain.repository are interfaces")
        void domainRepositoriesAreInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage("com.ziyara.backend.domain.repository..")
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().beInterfaces();
            rule.check(classes);
        }

        @Test
        @DisplayName("Classes ending 'RepositoryAdapter' reside in infrastructure.persistence.adapter")
        void repositoryAdaptersLiveInInfrastructure() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("RepositoryAdapter")
                    .should().resideInAPackage("com.ziyara.backend.infrastructure.persistence.adapter..");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("UseCase naming")
    class UseCaseNaming {

        @Test
        @DisplayName("Classes ending 'UseCase' reside in domain.usecase")
        void useCasesLiveInDomain() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("UseCase")
                    .and().areNotInterfaces()
                    .should().resideInAPackage("com.ziyara.backend.domain.usecase..");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("JPA entity naming")
    class JpaEntityNaming {

        @Test
        @DisplayName("Classes ending 'Jpa' reside in infrastructure.persistence.entity")
        void jpaEntitiesLiveInInfrastructure() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Jpa")
                    .and().areNotInterfaces()
                    .should().resideInAPackage("com.ziyara.backend.infrastructure.persistence.entity..");
            rule.check(classes);
        }

        @Test
        @DisplayName("No Jpa-suffixed class lives in the domain layer")
        void noJpaEntitiesInDomain() {
            ArchRule rule = noClasses()
                    .that().haveSimpleNameEndingWith("Jpa")
                    .should().resideInAPackage("com.ziyara.backend.domain..");
            rule.check(classes);
        }
    }
}
