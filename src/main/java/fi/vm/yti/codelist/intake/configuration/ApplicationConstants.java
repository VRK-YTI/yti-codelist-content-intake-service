package fi.vm.yti.codelist.intake.configuration;

public class ApplicationConstants {

    public static final String YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME = "serviceclassification";
    public static final String YTI_LANGUAGECODE_CODESCHEME = "languagecodes";
    public static final String DCAT_CODESCHEME = "dcat";
    private static final String[] INITIALIZATION_CODE_SCHEMES = { YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME, YTI_LANGUAGECODE_CODESCHEME, DCAT_CODESCHEME };

    public static String[] initializationCodeSchemes() {
        return INITIALIZATION_CODE_SCHEMES.clone();
    }
}
