package fi.vm.yti.cls.intake.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that contains methods for accessing AvoinData data resources.
 */
public class AvoinDataDataAccess {

    private static final Logger LOG = LoggerFactory.getLogger(AvoinDataDataAccess.class);

    /**
     * URL getter for avoindata.fi data by package name and resource name.
     */
    public String getAvoinDataPackageUrl(final String packageId, final String resourceName) {
        final String packageShowApiPath = "https://www.avoindata.fi/data/fi/api/3/action/package_show?id={packageId}";
        final HttpClient httpClient = HttpClientBuilder.create().build();
        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);
        final RestTemplate restTemplate = new RestTemplate(requestFactory);

        final Map<String, String> vars = new HashMap<>();
        vars.put("packageId", packageId);

        try {
            final String response = restTemplate.getForObject(packageShowApiPath, String.class, vars);
            final ObjectMapper mapper = new ObjectMapper();
            if (response != null && !response.isEmpty()) {
                try {
                    final JsonNode node = mapper.readTree(response);
                    final JsonNode resultNode = node.path("result");
                    final JsonNode resourcesNode = resultNode.path("resources");
                    if (resourcesNode.isArray()) {
                        final ArrayNode resourcesArray = (ArrayNode) resourcesNode;
                        for (final JsonNode resourceNode : resourcesArray) {
                            if (resourceName.equalsIgnoreCase(resourceNode.path("name").asText())) {
                                return resourceNode.path("url").asText();
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Avoin data package url generation failed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.error("Avoin data package url generation failed: " + e.getMessage());
        }
        return null;
    }

}
