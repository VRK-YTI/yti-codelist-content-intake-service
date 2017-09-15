package fi.vm.yti.cls.intake;

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

    public static void main(final String[] args) {
        final ApplicationContext context = SpringApplication.run(ContentIntakeServiceApplication.class, args);
        final ServiceInitializer serviceInitializer = (ServiceInitializer) context.getBean(ServiceInitializer.class);
        serviceInitializer.printLogo();
        serviceInitializer.initialize();
    }

}
