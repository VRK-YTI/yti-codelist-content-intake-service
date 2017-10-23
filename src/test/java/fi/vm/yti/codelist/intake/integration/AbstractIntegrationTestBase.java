package fi.vm.yti.codelist.intake.integration;

import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

abstract public class AbstractIntegrationTestBase {

    public static final String TEST_BASE_URL = "http://localhost";
    public static final String TEST_CODEREGISTRY_FILENAME = "v1_testcoderegistries.csv";
    public static final String TEST_CODESCHEME_FILENAME = "v1_testcodeschemes.csv";
    public static final String TEST_CODE_FILENAME = "v1_testcodes.csv";
    public static final String CODEREGISTRIES_FOLDER_NAME = "coderegistries";
    public static final String CODESCHEMES_FOLDER_NAME = "codeschemes";
    public static final String CODES_FOLDER_NAME = "codes";
    public static final String TEST_CODEREGISTRY_CODEVALUE = "testregistry1";
    public static final String TEST_CODESCHEME_CODEVALUE = "testscheme1";

    public String createApiUrl(final int serverPort,
                               final String apiPath) {
        return TEST_BASE_URL + ":" + serverPort + API_CONTEXT_PATH_INTAKE + API_BASE_PATH + API_PATH_VERSION_V1 + apiPath + "/";
    }
}
