package fi.vm.yti.cls.intake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableAutoConfiguration
@EnableJpaRepositories("fi.vm.yti.cls.*")
@EntityScan("fi.vm.yti.cls.*")
@ComponentScan({ "fi.vm.yti.cls.*" })
public class ContentIntakeServiceApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ContentIntakeServiceApplication.class);

    public static final String APP_VERSION = ContentIntakeServiceApplication.class.getPackage().getImplementationVersion();

    private static boolean INITIALIZE_ON_STARTUP = true;

    private static boolean ONLY_INDEX = false;


    public static void main(final String[] args) {

        final ApplicationContext context = SpringApplication.run(ContentIntakeServiceApplication.class, args);

        printLogo();

        final ServiceInitializer serviceInitializer = (ServiceInitializer) context.getBean(ServiceInitializer.class);

        if (INITIALIZE_ON_STARTUP) {
            serviceInitializer.initialize(ONLY_INDEX);
        }

    }


    /**
     * Application logo printout to log.
     */
    private static void printLogo() {

        LOG.info("");
        LOG.info("       .__                     .__        __          __           ");
        LOG.info("  ____ |  |   ______           |__| _____/  |______  |  | __ ____  ");
        LOG.info("_/ ___\\|  |  /  ___/   ______  |  |/    \\   __\\__  \\ |  |/ // __ \\ ");
        LOG.info("\\  \\___|  |__\\___ \\   /_____/  |  |   |  \\  |  / __ \\|    <\\  ___/ ");
        LOG.info(" \\___  >____/____  >           |__|___|  /__| (____  /__|_ \\\\___  >");
        LOG.info("     \\/          \\/                    \\/          \\/     \\/    \\/ ");
        LOG.info("                          .__              ");
        LOG.info("  ______ ______________  _|__| ____  ____  ");
        LOG.info(" /  ___// __ \\_  __ \\  \\/ /  |/ ___\\/ __ \\ ");
        LOG.info(" \\___ \\\\  ___/|  | \\/\\   /|  \\  \\__\\  ___/ ");
        LOG.info("/____  >\\___  >__|    \\_/ |__|\\___  >___  >");
        LOG.info("     \\/     \\/                    \\/    \\/ ");
        LOG.info("");
        LOG.info("                --- Version " + APP_VERSION + " starting up. --- ");
        LOG.info("");

    }
    
}
