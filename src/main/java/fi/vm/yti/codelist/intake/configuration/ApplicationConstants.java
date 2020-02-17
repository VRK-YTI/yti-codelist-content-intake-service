package fi.vm.yti.codelist.intake.configuration;

public interface ApplicationConstants {

    String YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME = "serviceclassification";
    String YTI_LANGUAGECODE_CODESCHEME = "languagecodes";
    String DCAT_CODESCHEME = "dcat";
    String[] INITIALIZATION_CODE_SCHEMES = { YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME, YTI_LANGUAGECODE_CODESCHEME, DCAT_CODESCHEME };

    static String[] initializationCodeSchemes() {
        return INITIALIZATION_CODE_SCHEMES.clone();
    }
}
