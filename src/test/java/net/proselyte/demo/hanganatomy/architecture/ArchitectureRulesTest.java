package net.proselyte.demo.hanganatomy.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.proselyte.demo.hanganatomy.client.PartnerClient;
import org.springframework.web.client.RestClient;

@AnalyzeClasses(
        packages = "net.proselyte.demo.hanganatomy",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule controllersDoNotCallRepositoriesDirectly =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule externalPartnerCallsGoThroughPartnerClient =
            noClasses().that().resideOutsideOfPackages("..client..", "..config..")
                    .should().dependOnClassesThat().haveFullyQualifiedName(RestClient.class.getName());

    @ArchTest
    static final ArchRule dashboardReadLayerMustNotDependOnPartnerClient =
            noClasses().that().resideInAPackage("..service..").and().haveSimpleNameContaining("ReadService")
                    .should().dependOnClassesThat().areAssignableTo(PartnerClient.class);
}
