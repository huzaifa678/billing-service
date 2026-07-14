package com.project.billing.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the hexagonal dependency direction (adapters → application → domain)
 * and keeps the domain and application layers free of framework/transport types.
 */
@AnalyzeClasses(
        packages = "com.project.billing",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application_or_adapters =
            noClasses().that().resideInAPackage("com.project.billing.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.project.billing.application..",
                            "com.project.billing.adapter.."
                    );

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters =
            noClasses().that().resideInAPackage("com.project.billing.application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.project.billing.adapter..");

    @ArchTest
    static final ArchRule domain_is_free_of_frameworks =
            noClasses().that().resideInAPackage("com.project.billing.domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.apache.avro..",
                            "io.grpc..",
                            "com.stripe.."
                    );

    @ArchTest
    static final ArchRule application_is_free_of_web_persistence_and_transport =
            noClasses().that().resideInAPackage("com.project.billing.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.web..",
                            "jakarta.persistence..",
                            "org.apache.avro..",
                            "io.grpc..",
                            "com.stripe..",
                            "com.project.subscription.."
                    );

    @ArchTest
    static final ArchRule domain_and_application_do_not_depend_on_bootstrap =
            noClasses().that()
                    .resideInAnyPackage("com.project.billing.domain..", "com.project.billing.application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.project.billing.config..");
}
