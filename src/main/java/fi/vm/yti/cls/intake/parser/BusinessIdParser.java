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

    private final BusinessIdRepository m_businessIdRepository;

    private final ApiUtils m_apiUtils;


    @Inject
    public BusinessIdParser(final ApiUtils apiUtils,
                            final BusinessIdRepository businessIdRepository) {

        m_apiUtils = apiUtils;
        m_businessIdRepository = businessIdRepository;

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
                        final String detailsUrl = resultNode.get("detailsUri").asText();
                        final String name = resultNode.get("name").asText();

                        final BusinessId businessId = createOrUpdateBusinessId(code, source, Status.VALID, name, companyFrom, detailsUrl);

                        list.add(businessId);
                    }
                }

            } catch (IOException e) {
                LOG.error("Parsing businessids failed: " + e.getMessage());
            }
        }

        return list;

    }

    public BusinessId createOrUpdateBusinessId(final String code,
                                               final String source,
                                               final Status status,
                                               final String name,
                                               final String companyFrom,
                                               final String detailsUrl) {

        final Date timeStamp = new Date(System.currentTimeMillis());
        final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_BUSINESSIDS, code);

        BusinessId businessId = m_businessIdRepository.findByCode(code);

        // Update
        if (businessId != null) {
            boolean hasChanges = false;
            if (!Objects.equals(businessId.getStatus(), status.toString())) {
                businessId.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getDetailsUrl(), detailsUrl)) {
                businessId.setDetailsUrl(detailsUrl);
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getUrl(), url)) {
                businessId.setUrl(url);
                hasChanges = true;
            }
            if (!Objects.equals(businessId.getNameFinnish(), name)) {
                businessId.setNameFinnish(name);
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
            businessId.setCode(code);
            businessId.setStatus(status.toString());
            businessId.setSource(source);
            businessId.setCreated(timeStamp);
            businessId.setNameFinnish(name);
            businessId.setCompanyForm(companyFrom);
            businessId.setUrl(url);
            businessId.setDetailsUrl(detailsUrl);
        }

        return businessId;

    }

}
