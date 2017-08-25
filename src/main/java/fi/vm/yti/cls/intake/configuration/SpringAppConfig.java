package fi.vm.yti.cls.intake.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@Configuration
@PropertySource(value = "classpath", ignoreResourceNotFound = true)
public class SpringAppConfig {

    @Value("${cls_content_intake_service_elastic_host}")
    private String m_elasticsearchHost;

    @Value("${cls_content_intake_service_elastic_port}")
    private Integer m_elasticsearchPort;

    @Value("${cls_content_intake_service_elastic_cluster}")
    private String m_clusterName;

    @Value(value = "${application.contextPath}")
    private String m_contextPath;


    public SpringAppConfig() {

    }


    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {

        return new PropertySourcesPlaceholderConfigurer();

    }


    @Bean
    public EmbeddedServletContainerFactory servletContainer() {

        final JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/notfound.html"));
        factory.setContextPath(m_contextPath);
        return factory;

    }


    @ConfigurationProperties(prefix = "hikari")
    @Bean
    public DataSource dataSource() {

        return new HikariDataSource();

    }


    @Bean
    protected Client elasticsearchClient() throws UnknownHostException {

        final TransportAddress address = new InetSocketTransportAddress(InetAddress.getByName(m_elasticsearchHost), m_elasticsearchPort);
        final Settings settings = Settings.builder().put("cluster.name", m_clusterName).put("client.transport.ignore_cluster_name", false).put("client.transport.sniff", false).build();
        try (PreBuiltTransportClient preBuiltTransportClient = new PreBuiltTransportClient(settings)) {
            return preBuiltTransportClient.addTransportAddress(address);
        }

    }

}
