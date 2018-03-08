package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.groupmanagement.GroupManagementUserRequest;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

@Component
@Path("/v1/groupmanagement")
@Api(value = "groupmanagement")
public class GroupManagementProxyResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(GroupManagementProxyResource.class);
    private AuthenticatedUserProvider authenticatedUserProvider;
    private GroupManagementProperties groupManagementProperties;

    @Inject
    public GroupManagementProxyResource(final GroupManagementProperties groupManagementProperties,
                                        final AuthenticatedUserProvider authenticatedUserProvider) {
        this.groupManagementProperties = groupManagementProperties;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @GET
    @Path("/requests")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Returns a list of user requests that the user has made.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response getUserRequests() {
        logApiRequest(LOG, METHOD_GET, API_PATH_VERSION_V1, API_PATH_GROUPMANAGEMENT + API_PATH_REQUESTS);
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }

        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);

        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        final String response = restTemplate.getForObject(createGroupManagementRequestsApiUrl(user.getEmail()), String.class);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        final Meta meta = new Meta();
        final ResponseWrapper<GroupManagementUserRequest> wrapper = new ResponseWrapper<>(meta);
        final Set<GroupManagementUserRequest> userRequests;
        try {
            userRequests = mapper.readValue(response, new TypeReference<Set<GroupManagementUserRequest>>() {
            });
            meta.setCode(200);
            meta.setResultCount(userRequests.size());
            wrapper.setResults(userRequests);
            return Response.ok(wrapper).build();
        } catch (final IOException e) {
            LOG.error("Error parsing userRequests from groupmanagement response! ", e);
            meta.setMessage("Error parsing userRequests from groupmanagement!");
            meta.setCode(500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
        }
    }

    @POST
    @Path("/request")
    @ApiOperation(value = "Sends user request to add user to an organization to groupmanagement service.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response sendUserRequest(@ApiParam(value = "UUID for the requested organization.", required = true) @QueryParam("organizationId") final String organizationId) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_GROUPMANAGEMENT + API_PATH_REQUEST);
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }

        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);

        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        final String requestUrl = createGroupManagementRequestApiUrl();

        final LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("email", user.getEmail());
        parameters.add("organizationId", organizationId);
        parameters.add("role", Role.CODE_LIST_EDITOR.toString());
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);

        final ResponseEntity response = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return Response.status(200).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String createGroupManagementRequestApiUrl() {
        return groupManagementProperties.getUrl() + GROUPMANAGEMENT_API_CONTEXT_PATH + GROUPMANAGEMENT_API_REQUEST;
    }

    private String createGroupManagementRequestsApiUrl(final String email) {
        return groupManagementProperties.getUrl() + GROUPMANAGEMENT_API_CONTEXT_PATH + GROUPMANAGEMENT_API_REQUESTS + "?email=" + email;
    }
}
