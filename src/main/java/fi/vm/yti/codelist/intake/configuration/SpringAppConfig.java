package fi.vm.yti.codelist.intake.configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.sql.DataSource;

import org.apache.catalina.connector.Connector;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.zaxxer.hikari.HikariDataSource;

@Component
@Configuration
@PropertySource(value = "classpath", ignoreResourceNotFound = true)
public class SpringAppConfig {

    @Value("${yti_codelist_content_intake_service_elastic_host}")
    private String elasticsearchHost;

    @Value("${yti_codelist_content_intake_service_elastic_port}")
    private Integer elasticsearchPort;

    @Value("${yti_codelist_content_intake_service_elastic_cluster}")
    private String clusterName;

    @Value(value = "${application.contextPath}")
    private String contextPath;

    private static final int CONNECTION_TIMEOUT = 10000;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer(@Value("${tomcat.ajp.port:}") Integer ajpPort) {
        final TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        tomcat.setContextPath(contextPath);
        tomcat.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/notfound.html"));
        if (ajpPort != null) {
            final Connector ajpConnector = new Connector("AJP/1.3");
            ajpConnector.setPort(ajpPort);
            ajpConnector.setSecure(false);
            ajpConnector.setAllowTrace(false);
            ajpConnector.setScheme("http");
            tomcat.addAdditionalTomcatConnectors(ajpConnector);
        }
        return tomcat;
    }

    @ConfigurationProperties(prefix = "hikari")
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    @Bean
    @SuppressWarnings("resource")
    protected Client elasticSearchClient() throws UnknownHostException {
        final TransportAddress address = new TransportAddress(InetAddress.getByName(elasticsearchHost), elasticsearchPort);
        final Settings settings = Settings.builder().put("cluster.name", clusterName).put("client.transport.ignore_cluster_name", false).put("client.transport.sniff", false).build();
        return new PreBuiltTransportClient(settings).addTransportAddress(address);
    }

    @Bean
    ClientHttpRequestFactory httpRequestFactory() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECTION_TIMEOUT);
        requestFactory.setReadTimeout(CONNECTION_TIMEOUT);
        return requestFactory;
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate(httpRequestFactory());
    }
}
