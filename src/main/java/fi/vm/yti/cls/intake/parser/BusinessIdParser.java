package fi.vm.yti.cls.intake.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fi.vm.yti.cls.common.model.BusinessId;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.jpa.BusinessIdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Class that handles parsing of business ids from source data.
 */
@Service
public class BusinessIdParser {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessIdParser.class);
    private final BusinessIdRepository businessIdRepository;
    private final ApiUtils apiUtils;

    @Inject
    public BusinessIdParser(final ApiUtils apiUtils,
                            final BusinessIdRepository businessIdRepository) {
        this.apiUtils = apiUtils;
        this.businessIdRepository = businessIdRepository;
    }

    public List<BusinessId> parseBusinessIdsFromJsonArray(final String source,
                                                          final String data) {
        final List<BusinessId> list = new ArrayList<>();
        final ObjectMapper mapper = new ObjectMapper();

        if (data != null && !data.isEmpty()) {

            try {
                final JsonNode node = mapper.readTree(data);
                final JsonNode resultsNode = node.path("results");

                if (resultsNode.isArray()) {
                    final ArrayNode resultsArray = (ArrayNode) resultsNode;

                    for (final JsonNode resultNode : resultsArray) {
                        final String code = resultNode.path("businessId").asText();
                        final String companyFrom = resultNode.path("companyForm").asText();
                        final String detailsUri = resultNode.get("detailsUri").asText();
                        final String name = resultNode.get("name").asText();

                        final BusinessId businessId = createOrUpdateBusinessId(code, source, Status.VALID, name, companyFrom, detailsUri);

                        list.add(businessId);
                    }
                }

            } catch (IOException e) {
                LOG.error("Parsing businessids failed: " + e.getMessage());
            }
        }
        return list;
    }

    public BusinessId createOrUpdateBusinessId(final String codeValue,
                                               final String source,
                                               final Status status,
                                               final String name,
                                               final String companyFrom,
                                               final String detailsUri) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final String url = apiUtils.createResourceUrl(ApiConstants.API_PATH_BUSINESSIDS, codeValue);

        BusinessId businessId = businessIdRepository.findByCodeValue(codeValue);

        // Update
        if (businessId != null) {
            boolean hasChanges = false;
            if (!Objects.equals(businessId.getStatus(), status.toString())) {
                businessId.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getDetailsUri(), detailsUri)) {
                businessId.setDetailsUri(detailsUri);
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getUri(), url)) {
                businessId.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getPrefLabelFi(), name)) {
                businessId.setPrefLabelFi(name);
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getCompanyForm(), companyFrom)) {
                businessId.setCompanyForm(companyFrom);
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getSource(), source)) {
                businessId.setSource(source);
                hasChanges = true;
            }
            if (hasChanges) {
                businessId.setModified(timeStamp);
            }
        // Create
        } else {
            businessId = new BusinessId();
            businessId.setId(UUID.randomUUID().toString());
            businessId.setCodeValue(codeValue);
            businessId.setStatus(status.toString());
            businessId.setSource(source);
            businessId.setCreated(timeStamp);
            businessId.setPrefLabelFi(name);
            businessId.setCompanyForm(companyFrom);
            businessId.setUri(url);
            businessId.setDetailsUri(detailsUri);
        }
        return businessId;
    }

}
