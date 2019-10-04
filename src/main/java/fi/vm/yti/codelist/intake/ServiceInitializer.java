package fi.vm.yti.codelist.intake;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import fi.vm.yti.codelist.intake.configuration.VersionInformation;
import fi.vm.yti.codelist.intake.data.YtiDataAccess;
import fi.vm.yti.codelist.intake.groupmanagement.OrganizationUpdater;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.UserService;

@Component
public class ServiceInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceInitializer.class);
    private final YtiDataAccess ytiDataAccess;
    private final Indexing indexing;
    private final OrganizationUpdater organizationUpdater;
    private final UserService userService;
    private final VersionInformation versionInformation;

    private boolean initializing;

    @Inject
    public ServiceInitializer(final VersionInformation versionInformation,
                              final Indexing indexing,
                              final YtiDataAccess ytiDataAccess,
                              final OrganizationUpdater organizationUpdater,
                              final UserService userService) {
        this.versionInformation = versionInformation;
        this.indexing = indexing;
        this.ytiDataAccess = ytiDataAccess;
        this.organizationUpdater = organizationUpdater;
        this.userService = userService;
    }

    @Override
    public void run(final ApplicationArguments applicationArguments) {
        initialize();
    }

    private void initialize() {
        initializing = true;
        printLogo();
        LOG.info("*** Initializing data. ***");
        indexing.cleanRunningIndexingBookkeeping();
        LOG.info("*** Updating organizations. ***");
        organizationUpdater.updateOrganizations();
        LOG.info("*** Updating users. ***");
        userService.updateUsers();
        final Stopwatch watch = Stopwatch.createStarted();
        ytiDataAccess.initializeOrRefresh();
        LOG.info(String.format("*** Database population took: %s. ***", watch));
        final Stopwatch indexWatch = Stopwatch.createStarted();
        indexing.reIndexEverything();
        LOG.info(String.format("*** Elastic indexing took: %s. ***", indexWatch));
        LOG.info(String.format("*** Data initialization complete, took %s. ***", watch));
        initializing = false;
    }

    public boolean isInitializing() {
        return initializing;
    }

    private void printLogo() {
        LOG.info("");
        LOG.info("          __  .__          .__        __          __           ");
        LOG.info(" ___.__._/  |_|__|         |__| _____/  |______  |  | __ ____  ");
        LOG.info("<   |  |\\   __\\  |  ______ |  |/    \\   __\\__  \\ |  |/ // __ \\ ");
        LOG.info(" \\___  | |  | |  | /_____/ |  |   |  \\  |  / __ \\|    <\\  ___/ ");
        LOG.info(" / ____| |__| |__|         |__|___|  /__| (____  /__|_ \\\\___  >");
        LOG.info(" \\/                                \\/          \\/     \\/    \\/ ");
        LOG.info("                          .__              ");
        LOG.info("  ______ ______________  _|__| ____  ____  ");
        LOG.info(" /  ___// __ \\_  __ \\  \\/ /  |/ ___\\/ __ \\ ");
        LOG.info(" \\___ \\\\  ___/|  | \\/\\   /|  \\  \\__\\  ___/ ");
        LOG.info("/____  >\\___  >__|    \\_/ |__|\\___  >___  >");
        LOG.info("     \\/     \\/                    \\/    \\/ ");
        LOG.info("");
        LOG.info(String.format("                --- Version %s starting up. --- ", versionInformation.getVersion()));
        LOG.info("");
    }
}
