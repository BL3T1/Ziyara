package com.ziyara.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Phase 3: Module boundary rules (MODULAR_MONOLITH_STRUCTURE).
 * - Only modules.*.api may be used from outside the module.
 * - modules.*.service must not be accessed from application, presentation, or other modules.
 *
 * @see CleanArchitectureDddTest GROUP 9 for the full module boundary rule set.
 *      This class retains the single original rule as a fast smoke-check.
 */
class ModuleBoundariesArchTest {

    private static JavaClasses basePackage;

    @BeforeAll
    static void setup() {
        basePackage = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.ziyara.backend");
    }

    @Test
    void modules_service_must_not_be_accessed_from_outside_their_module() {
        // Classes in ..modules.<X>.service.. may only be accessed by classes in ..modules.<X>..
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .or().resideInAPackage("..presentation..")
                .or().resideInAPackage("..infrastructure..")
                .should().accessClassesThat().resideInAPackage("..modules..service..")
                .because("Cross-module calls must use module API (..modules.*.api) only");
        rule.check(basePackage);
    }
}
