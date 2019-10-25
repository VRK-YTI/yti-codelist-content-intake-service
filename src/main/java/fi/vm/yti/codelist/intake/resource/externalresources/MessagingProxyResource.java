package fi.vm.yti.codelist.intake.resource.externalresources;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.configuration.MessagingProperties;
import fi.vm.yti.codelist.intake.dto.MessagingUserDTO;
import fi.vm.yti.codelist.intake.dto.SubscriptionRequestDTO;
import fi.vm.yti.codelist.intake.dto.SubscriptionTypeRequestDTO;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.resource.AbstractBaseResource;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_BASE_PATH;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_VERSION;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
@Path("/v1/messaging")
public class MessagingProxyResource implements AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(MessagingProxyResource.class);
    private final RestTemplate restTemplate;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final MessagingProperties messagingProperties;

    @Inject
    public MessagingProxyResource(final MessagingProperties messagingProperties,
                                  final AuthenticatedUserProvider authenticatedUserProvider,
                                  final RestTemplate restTemplate) {
        this.messagingProperties = messagingProperties;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.restTemplate = restTemplate;
    }

    @GET
    @Path("/user")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Returns the user and their subscription details.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns user details from the messaging microservice and lists subscribed resources."),
        @ApiResponse(responseCode = "401", description = "Unauthorized."),
        @ApiResponse(responseCode = "404", description = "No user found in messaging API.")
    })
    public Response getUser() {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final ResponseEntity<String> response;
        try {
            final String userPath = createMessagingUserPath(user.getId());
            response = restTemplate.getForEntity(userPath, String.class);
        } catch (final Exception e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (response.getStatusCode() == HttpStatus.OK) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
            try {
                final MessagingUserDTO messagingUserData = mapper.readValue(response.getBody(), new TypeReference<MessagingUserDTO>() {
                });
                return Response.ok(messagingUserData).build();
            } catch (final IOException e) {
                LOG.error("Error parsing Messaging User data from messaging response!", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_CANT_REACH_MESSAGING_API));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_CANT_REACH_MESSAGING_API));
        }
    }

    @POST
    @Path("/subscriptions")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Adds messaging subscription to logged in user to given uri.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns user details."),
        @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    public Response getOrAddSubcription(@Parameter(description = "Subscription request parameters as JSON payload.") @RequestBody final String subscriptionRequest,
                                        @Context HttpServletRequest httpServletrequest) {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final ResponseEntity<String> response;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_UTF8);
        headers.add("cookie", httpServletrequest.getHeader("cookie"));
        final String body = createSubscriptionRequestBody(subscriptionRequest, user.getId());
        final HttpEntity<String> request = new HttpEntity<>(body, headers);
        try {
            final String subscriptionPath = createMessagingSubscriptionsPath();
            response = restTemplate.postForEntity(subscriptionPath, request, String.class);
        } catch (final Exception e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (response.getStatusCode() == HttpStatus.OK) {
            return Response.ok().build();
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    @POST
    @Path("/user/subscriptiontype")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Change user subscription type.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns user details."),
        @ApiResponse(responseCode = "401", description = "Unauthorized."),
        @ApiResponse(responseCode = "404", description = "User not found.")
    })
    public Response setUserSubscriptionType(@Parameter(description = "Subscription type request parameters as JSON payload.") @RequestBody final String subscriptionTypeRequest,
                                            @Context HttpServletRequest httpServletrequest) {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final ResponseEntity<String> response;
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_UTF8);
        headers.add("cookie", httpServletrequest.getHeader("cookie"));
        final String body = createSubscriptionTypeRequestBody(subscriptionTypeRequest, user.getId());
        final HttpEntity<String> request = new HttpEntity<>(body, headers);
        try {
            final String subscriptionPath = createMessagingUserSubscriptionTypePath(user.getId());
            response = restTemplate.postForEntity(subscriptionPath, request, String.class);
        } catch (final Exception e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (response.getStatusCode() == HttpStatus.OK) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
            final MessagingUserDTO messagingUserData;
            try {
                messagingUserData = mapper.readValue(response.getBody(), new TypeReference<MessagingUserDTO>() {
                });
                return Response.ok(messagingUserData).build();
            } catch (final IOException e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private String createMessagingSubscriptionsPath() {
        return createMessagingBasePath() + "/subscriptions";
    }

    private String createMessagingUserPath(final UUID userId) {
        return createMessagingBasePath() + "/users/" + userId.toString();
    }

    private String createMessagingUserSubscriptionTypePath(final UUID userId) {
        return createMessagingUserPath(userId) + "/subscriptiontype";
    }

    private String createMessagingBasePath() {
        return messagingProperties.getUrl() + "/messaging-api" + API_BASE_PATH + "/" + API_VERSION;
    }

    private String createSubscriptionRequestBody(final String subscriptionRequest,
                                                 final UUID userId) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final SubscriptionRequestDTO subscriptionRequestDto = mapper.readValue(subscriptionRequest, new TypeReference<SubscriptionRequestDTO>() {
            });
            subscriptionRequestDto.setUserId(userId);
            return mapper.writeValueAsString(subscriptionRequestDto);
        } catch (final Exception e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private String createSubscriptionTypeRequestBody(final String subscriptionTypeRequest,
                                                     final UUID userId) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final SubscriptionTypeRequestDTO subscriptionTypeRequestDto = mapper.readValue(subscriptionTypeRequest, new TypeReference<SubscriptionTypeRequestDTO>() {
            });
            subscriptionTypeRequestDto.setUserId(userId);
            return mapper.writeValueAsString(subscriptionTypeRequestDto);
        } catch (final Exception e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }
}
