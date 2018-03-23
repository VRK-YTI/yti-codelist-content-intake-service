package fi.vm.yti.codelist.intake.groupmanagement;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.configuration.ImpersonateProperties;
import static fi.vm.yti.codelist.common.constants.ApiConstants.GROUPMANAGEMENT_API_CONTEXT_PATH;
import static fi.vm.yti.codelist.common.constants.ApiConstants.GROUPMANAGEMENT_API_USERS;
import static org.springframework.http.HttpMethod.GET;

@Component
public class ImpersonateUserService {

    private static final Logger LOG = LoggerFactory.getLogger(ImpersonateUserService.class);
    private final GroupManagementProperties groupManagementProperties;
    private final ImpersonateProperties fakeLoginProperties;
    private final RestTemplate restTemplate;

    @Inject
    public ImpersonateUserService(final GroupManagementProperties groupManagementProperties,
                                  final ImpersonateProperties fakeLoginProperties,
                                  final RestTemplate restTemplate) {
        this.groupManagementProperties = groupManagementProperties;
        this.fakeLoginProperties = fakeLoginProperties;
        this.restTemplate = restTemplate;
    }

    @NotNull
    public List<GroupManagementUser> getUsers() {
        if (fakeLoginProperties.isAllowed()) {
            String url = groupManagementProperties.getUrl() + GROUPMANAGEMENT_API_CONTEXT_PATH + GROUPMANAGEMENT_API_USERS;
            LOG.info("URL " + url);
            return restTemplate.exchange(url, GET, null, new ParameterizedTypeReference<List<GroupManagementUser>>() {
            }).getBody();
        } else {
            return Collections.emptyList();
        }
    }
}
