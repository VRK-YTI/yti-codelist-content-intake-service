package fi.vm.yti.codelist.intake.resource.externalresources;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.configuration.TerminologyProperties;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.UnreachableTerminologyApiException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.resource.AbstractBaseResource;
import fi.vm.yti.codelist.intake.terminology.Attribute;
import fi.vm.yti.codelist.intake.terminology.Concept;
import fi.vm.yti.codelist.intake.terminology.ConceptSuggestionRequest;
import fi.vm.yti.codelist.intake.terminology.ConceptSuggestionResponse;
import fi.vm.yti.codelist.intake.terminology.Vocabulary;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_401;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_ERROR_ENCODING_STRING;

@Component
@Path("/v1/terminology")
public class TerminologyProxyResource implements AbstractBaseResource {

    public static final String ERROR_CREATING_A_CONCEPT_IN_TERMINOLOGY_API = "Error creating a concept in terminology-api!";
    private static final Logger LOG = LoggerFactory.getLogger(TerminologyProxyResource.class);
    private final RestTemplate restTemplate;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final TerminologyProperties terminologyProperties;

    @Inject
    public TerminologyProxyResource(final TerminologyProperties terminologyProperties,
                                    final AuthenticatedUserProvider authenticatedUserProvider,
                                    final RestTemplate restTemplate) {
        this.terminologyProperties = terminologyProperties;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.restTemplate = restTemplate;
    }

    @GET
    @Path("/vocabularies")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Returns the complete list of existing vocabularies.")
    @ApiResponse(responseCode = "200", description = "Returns success.")
    @SuppressWarnings("Duplicates")
    public Response getVocabularies() {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }


        Map<String, String> params = new HashMap<>();

        if (!user.isSuperuser()) {
            Set<String> usersOrganizations = new HashSet<>();
            Map rolesInOrganisations = user.getRolesInOrganizations();
            rolesInOrganisations.forEach((k, v) -> {
                usersOrganizations.add(k.toString());
            });

            StringBuilder userOrgCsl = new StringBuilder();
            int counter = 1;
            for (String org : usersOrganizations) {
                userOrgCsl.append(org);
                if (counter < usersOrganizations.size()) {
                    userOrgCsl.append(",");
                }
                counter++;
            }
            params.put("includeIncompleteFrom", userOrgCsl.toString());
            params.put("includeIncomplete", "false");
        } else {
            params.put("includeIncompleteFrom", "");
            params.put("includeIncomplete", "true");
        }


        final ResponseEntity response;
        try {
            response = restTemplate.exchange(createTerminologyVocabulariesApiUrl(), HttpMethod.GET, null, String.class, params);
        } catch (final Exception e) {
            LOG.error("Error getting vocabularies from terminology response!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }

        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        final Meta meta = new Meta();
        final ResponseWrapper<Vocabulary> wrapper = new ResponseWrapper<>(meta);
        final Set<Vocabulary> vocabularies;
        try {
            vocabularies = this.parseVocabulariesFromResponse(response);
            meta.setCode(200);
            meta.setResultCount(vocabularies.size());
            wrapper.setResults(vocabularies);
            return Response.ok(wrapper).build();
        } catch (final Exception e) {
            LOG.error("Error parsing vocabularies from terminology response!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
    }

    private String createTerminologyVocabulariesApiUrl() {
        String vocabUrl = terminologyProperties.getUrl() + TERMINOLOGY_API_CONTEXT_PATH + API_PATH_CONTAINERS  + "?includeIncomplete={includeIncomplete}&includeIncompleteFrom={includeIncompleteFrom}";
        LOG.info("Terminology vocabularies URL created in Codelist TerminologyProxyResource is " + vocabUrl);
        return vocabUrl;
    }

    @GET
    @Path("/concepts")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Returns a filtered list of concepts")
    @ApiResponse(responseCode = "200", description = "Returns success.")
    @SuppressWarnings("Duplicates")
    public Response getConcepts(@Parameter(description = "Search term for filtering response data.", in = ParameterIn.QUERY) @QueryParam("searchTerm") String searchTerm,
                                @Parameter(description = "Vocabulary ID for fetching concepts in specific vocabulary.", in = ParameterIn.QUERY) @QueryParam("containerUri") String containerUri,
                                @Parameter(description = "Status for filtering response data based on resource status.", in = ParameterIn.QUERY) @QueryParam("status") String status,
                                @Parameter(description = "Language parameter that is used in sorting data.", in = ParameterIn.QUERY) @QueryParam("language") String language) {

        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        Map<String, String> params = new HashMap<>();

        if (!user.isSuperuser()) {
            Set<String> usersOrganizations = new HashSet<>();
            Map rolesInOrganisations = user.getRolesInOrganizations();
            rolesInOrganisations.forEach((k, v) -> {
                usersOrganizations.add(k.toString());
            });

            StringBuilder userOrgCsl = new StringBuilder();
            int counter = 1;
            for (String org : usersOrganizations) {
                userOrgCsl.append(org);
                if (counter < usersOrganizations.size()) {
                    userOrgCsl.append(",");
                }
                counter++;
            }
            params.put("includeIncompleteFrom", userOrgCsl.toString());
            params.put("includeIncomplete", "false");
        } else {
            params.put("includeIncompleteFrom", "");
            params.put("includeIncomplete", "true");
        }

        final Meta meta = new Meta();
        final ResponseWrapper<Concept> wrapper = new ResponseWrapper<>(meta);
        ResponseEntity response = null;
        try {
            params.put("status", status);
            params.put("searchTerm", searchTerm);
            params.put("container", containerUri);
            if (!language.equals("all_selected") && !language.isEmpty()) {
                params.put("language", language);
            } else {
                params.put("language", "");
            }
            ParameterizedTypeReference<String> parameterizedTypeReference =
                new ParameterizedTypeReference<String>() {
                };
            response = restTemplate.exchange(createTerminologyConceptsApiUrl(), HttpMethod.GET, null, parameterizedTypeReference, params);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // ok to continue
            } else {
                LOG.error("Error getting concepts from terminology-api!", e);
                throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
            }
        } catch (Exception e) {
            LOG.error("Error getting concepts from terminology-api!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        final Set<Concept> concepts;
        try {
            concepts = this.parseResourcesFromResponse(response);
            meta.setCode(200);
            meta.setResultCount(concepts.size());
            wrapper.setResults(concepts);
            return Response.ok(wrapper).build();
        } catch (final Exception e) {
            LOG.error("Error parsing concepts from terminology response!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
    }

    private String createTerminologyConceptsApiUrl() {
        String conceptsUrl = terminologyProperties.getUrl() + TERMINOLOGY_API_CONTEXT_PATH + API_PATH_RESOURCES + "?language={language}&status={status}&container={container}&searchTerm={searchTerm}&includeIncomplete={includeIncomplete}&includeIncompleteFrom={includeIncompleteFrom}";
        LOG.info("Terminology conceptsUrl created in Codelist TerminologyProxyResource is " + conceptsUrl);
        return conceptsUrl;
    }

    @POST
    @Path("/suggestion")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Creates a concept in Controlled Vocabularies and returns it.")
    @ApiResponse(responseCode = "200", description = "Returns success.")
    @SuppressWarnings("Duplicates")
    public Response suggestAConcept(@Parameter(description = "URI of the Terminology to make the suggestion in.", in = ParameterIn.QUERY) @QueryParam("terminologyUri") final String terminologyUri,
                                    @Parameter(description = "Content language for the defined suggestion.", in = ParameterIn.QUERY) @QueryParam("contentLanguage") final String contentLanguage,
                                    @Parameter(description = "Concept suggestion text.", in = ParameterIn.QUERY) @QueryParam("suggestion") final String suggestion,
                                    @Parameter(description = "Definition for the concept suggestion.", in = ParameterIn.QUERY) @QueryParam("definition") String suggestedDefinition,
                                    @Context HttpServletRequest httpServletrequest) {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        final Meta meta = new Meta();
        final ResponseWrapper<Concept> wrapper = new ResponseWrapper<>(meta);
        ResponseEntity response = null;
        Attribute prefLabel = new Attribute(contentLanguage, suggestion);
        Attribute definition = new Attribute(contentLanguage, suggestedDefinition);
        ConceptSuggestionRequest conceptSuggestionRequest = new ConceptSuggestionRequest(prefLabel, definition, user.getId(), terminologyUri);

        final ObjectMapper objectMapper = new ObjectMapper();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_UTF8);
        headers.add("cookie", httpServletrequest.getHeader("cookie"));

        HttpEntity<String> request;
        try {
            request = new HttpEntity<>(objectMapper.writeValueAsString(conceptSuggestionRequest), headers);
        } catch (JsonProcessingException e) {
            LOG.error(ERROR_CREATING_A_CONCEPT_IN_TERMINOLOGY_API, e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
        try {
            response = restTemplate.exchange(createTerminologyConceptSuggestionApiUrl(), HttpMethod.POST, request
                , String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // ok to continue
            } else {
                LOG.error(ERROR_CREATING_A_CONCEPT_IN_TERMINOLOGY_API, e);
                throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
            }
        } catch (Exception e) {
            LOG.error(ERROR_CREATING_A_CONCEPT_IN_TERMINOLOGY_API, e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));

        try {
            meta.setCode(200);
            meta.setResultCount(1);
            Set<Concept> resultAsConcept = new HashSet<>(); //always just one result in this Set.
            Concept theNewConcept = parseTheNewConceptFromResponse(response);
            resultAsConcept.add(theNewConcept);

            wrapper.setResults(new HashSet<>(resultAsConcept));
            return Response.ok(wrapper).build();
        } catch (final Exception e) {
            LOG.error("Error parsing conceptSuggestion from terminology response!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
    }

    private String createTerminologyConceptSuggestionApiUrl() {
        String conceptSuggestionUrl = terminologyProperties.getUrl() + TERMINOLOGY_API_CONCEPT_SUGGESTION_CONTEXT_PATH + "/terminology/conceptSuggestion";
        LOG.info("Terminology conceptSuggestionUrl created in Codelist TerminologyProxyResource is " + conceptSuggestionUrl);
        return conceptSuggestionUrl;
    }

    private Set<Vocabulary> parseVocabulariesFromResponse(final ResponseEntity response) {
        final Object responseBody = response.getBody();
        if (responseBody != null) {
            try {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
                final String data = responseBody.toString();
                final JsonNode jsonNode = mapper.readTree(data);
                final String dataString;
                if (!jsonNode.isArray() && jsonNode.has("results")) {
                    dataString = jsonNode.get("results").toString();
                } else {
                    dataString = data;
                }
                return mapper.readValue(dataString, new TypeReference<Set<Vocabulary>>() {
                });
            } catch (final IOException e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to parse vocabularies!"));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to parse vocabularies!"));
        }
    }

    private Concept parseTheNewConceptFromResponse(final ResponseEntity response) {
        final Object responseBody = response.getBody();
        if (responseBody != null) {
            try {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
                final String data = responseBody.toString();
                ConceptSuggestionResponse conceptSuggestionResponse = mapper.readValue(data, new TypeReference<ConceptSuggestionResponse>() {
                });
                return new Concept(conceptSuggestionResponse);
            } catch (final IOException e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to parse resources!"));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to parse resources!"));
        }
    }

    private Set<Concept> parseResourcesFromResponse(final ResponseEntity response) {
        final Object responseBody = response.getBody();
        if (responseBody != null) {
            try {
                final ObjectMapper mapper = new ObjectMapper();
                mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
                final String data = responseBody.toString();
                final JsonNode jsonNode = mapper.readTree(data);
                final String dataString;
                if (!jsonNode.isArray() && jsonNode.has("results")) {
                    dataString = jsonNode.get("results").toString();
                } else {
                    return new HashSet<Concept>(); // in case nothing is found
                }
                return mapper.readValue(dataString, new TypeReference<Set<Concept>>() {
                });
            } catch (final IOException e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to parse resources!"));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to parse resources!"));
        }
    }

    String urlEncodeString(final String string) {
        try {
            final String stringToDecode = string.replaceAll("\\+", "%2b");
            return URLEncoder.encode(stringToDecode, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ERROR_ENCODING_STRING));
        }
    }
}
