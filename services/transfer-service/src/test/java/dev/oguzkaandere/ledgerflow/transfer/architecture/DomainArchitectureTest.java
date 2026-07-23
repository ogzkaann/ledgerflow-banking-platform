package dev.oguzkaandere.ledgerflow.transfer.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.oguzkaandere.ledgerflow.transfer", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainArchitectureTest {
    @ArchTest
    static final ArchRule DOMAIN_IS_ISOLATED = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.servlet..",
                    "com.fasterxml.jackson..",
                    "..adapter..",
                    "..application..",
                    "dev.oguzkaandere.ledgerflow.account..");
}
