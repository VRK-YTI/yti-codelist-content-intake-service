package fi.vm.yti.codelist.intake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAutoConfiguration
@EnableJpaRepositories("fi.vm.yti.codelist.*")
@EnableTransactionManagement
@EntityScan("fi.vm.yti.codelist.*")
@ComponentScan({"fi.vm.yti.codelist.*"})
public class ContentIntakeServiceApplication {

    public static void main(final String[] args) {
        final ApplicationContext context = SpringApplication.run(ContentIntakeServiceApplication.class, args);
        final ServiceInitializer serviceInitializer = context.getBean(ServiceInitializer.class);
        serviceInitializer.printLogo();
        serviceInitializer.initialize();
    }
}