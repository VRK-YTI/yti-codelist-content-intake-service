package fi.vm.yti.codelist.intake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "fi.vm.yti.codelist.*")
@EnableJpaRepositories("fi.vm.yti.codelist.*")
@EnableTransactionManagement
@EntityScan("fi.vm.yti.codelist.*")
@EnableCaching
public class ContentIntakeServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ContentIntakeServiceApplication.class, args);
    }
}