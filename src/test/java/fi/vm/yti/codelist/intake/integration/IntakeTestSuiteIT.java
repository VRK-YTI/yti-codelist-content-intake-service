package fi.vm.yti.codelist.intake.integration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    InitializeDataT1.class,
    CodeRegistryResourceT2.class,
    CodeSchemeResourceT3.class,
    CodeSchemeDuplicateTestT4.class,
    CodeResourceT5.class,
    CodeBroaderUploadTestT6.class,
    CodeDuplicateUploadTestT7.class,
    DoubleHeaderUploadTestT8.class,
    InvalidIdUploadTestT9.class,
    CodeSchemeBadInformationDomainT10.class,
    CodeSchemeCaseTestT11.class,
    CodeCaseTestT12.class,
    CodeValueTestT13.class,
    CodeSchemeExtensionTestT14.class,
    CodeSchemeDcatTestT15.class,
    CodeSchemeWithExtensionsTestT16.class,
    CodeSchemeCaseTestT17.class
})
public class IntakeTestSuiteIT {

}
