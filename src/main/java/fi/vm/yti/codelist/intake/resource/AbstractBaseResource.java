package fi.vm.yti.codelist.intake.resource;

import org.slf4j.Logger;

public class AbstractBaseResource {

    public void logApiRequest(final Logger logger,
                              final String method,
                              final String apiVersionPath,
                              final String apiPath) {
        logger.info(method + " " + apiVersionPath + apiPath + " requested!");
    }
}
