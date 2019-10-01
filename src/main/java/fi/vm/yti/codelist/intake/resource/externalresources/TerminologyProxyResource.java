package fi.vm.yti.codelist.intake.resource.externalresources;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.configuration.TerminologyProperties;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.UnreachableTerminologyApiException;
import fi.vm.yti.codelist.intake.resource.AbstractBaseResource;
import fi.vm.yti.codelist.intake.terminology.Attribute;
import fi.vm.yti.codelist.intake.terminology.Concept;
import fi.vm.yti.codelist.intake.terminology.ConceptSuggestion;
import fi.vm.yti.codelist.intake.terminology.Vocabulary;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_401;

@Component
@Path("/v1/terminology")
@Api(value = "terminology")
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
    @ApiOperation(value = "Returns the complete list of existing vocabularies.")
    @ApiResponse(code = 200, message = "Returns success.")
    @SuppressWarnings("Duplicates")
    public Response getVocabularies() {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        final String response;
        try {
            response = restTemplate.getForObject(createTerminologyVocabulariesApiUrl(), String.class);
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
            vocabularies = mapper.readValue(response, new TypeReference<Set<Vocabulary>>() {
            });
            meta.setCode(200);
            meta.setResultCount(vocabularies.size());
            wrapper.setResults(vocabularies);
            return Response.ok(wrapper).build();
        } catch (final IOException e) {
            LOG.error("Error parsing vocabularies from terminology response!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
    }

    private String createTerminologyVocabulariesApiUrl() {
        String vocabUrl = terminologyProperties.getUrl() + API_PATH_TERMINOLOGY + TERMINOLOGY_API_CONTEXT_PATH + API_PATH_CONTAINERS;
        LOG.info("Terminology vocabularies URL created in Codelist TerminologyProxyResource is " + vocabUrl);
        return vocabUrl;
    }

    @GET
    @Path("/concepts")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Returns a filtered list of concepts")
    @ApiResponse(code = 200, message = "Returns success.")
    @SuppressWarnings("Duplicates")
    public Response getConcepts(@QueryParam("searchTerm") String searchTerm,
                                @QueryParam("vocabularyId") String vocabularyId,
                                @QueryParam("status") String status,
                                @QueryParam("language") String language) {

        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        final Meta meta = new Meta();
        final ResponseWrapper<Concept> wrapper = new ResponseWrapper<>(meta);
        String response;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("status", status);
            params.put("searchTerm", searchTerm);
            params.put("vocabularyId", vocabularyId);
            if (!language.equals("all_selected") && !language.isEmpty()) {
                params.put("language", language);
            } else {
                params.put("language", "");
            }
            response = restTemplate.getForObject(createTerminologyConceptsApiUrl(), String.class, params);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // ok to continue
                response = "[]";
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
            concepts = mapper.readValue(response, new TypeReference<Set<Concept>>() {
            });
            meta.setCode(200);
            meta.setResultCount(concepts.size());
            wrapper.setResults(concepts);
            return Response.ok(wrapper).build();
        } catch (final IOException e) {
            LOG.error("Error parsing concepts from terminology response!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
    }

    private String createTerminologyConceptsApiUrl() {
        String conceptsUrl = terminologyProperties.getUrl() + API_PATH_TERMINOLOGY + TERMINOLOGY_API_CONTEXT_PATH + API_PATH_CONCEPTS + "?language={language}&status={status}&vocabularyId={vocabularyId}&searchTerm={searchTerm}";
        LOG.info("Terminology conceptsUrl created in Codelist TerminologyProxyResource is " + conceptsUrl);
        return conceptsUrl;
    }

    @POST
    @Path("/suggestion/vocabulary/{vocabularyId}/language/{contentLanguage}/suggestion/{suggestion}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Creates a concept in Controlled Vocabularies and returns it.")
    @ApiResponse(code = 200, message = "Returns success.")
    @SuppressWarnings("Duplicates")
    public Response suggestAConcept(@PathParam("vocabularyId") final String vocabularyId,
                                    @PathParam("contentLanguage") final String contentLanguage,
                                    @PathParam("suggestion") final String suggestion,
                                    @RequestBody String suggestedDefinition,
                                    @Context HttpServletRequest httpServletrequest) {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        final Meta meta = new Meta();
        final ResponseWrapper<Concept> wrapper = new ResponseWrapper<>(meta);
        String response;
        Attribute prefLabel = new Attribute(contentLanguage, suggestion);
        Attribute definition = new Attribute(contentLanguage, suggestedDefinition);
        ConceptSuggestion conceptSuggestion = new ConceptSuggestion(prefLabel, definition, UUID.fromString(vocabularyId));
        conceptSuggestion.setCreator(user.getId());

        final ObjectMapper objectMapper = new ObjectMapper();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_UTF8);
        headers.add("cookie", httpServletrequest.getHeader("cookie"));

        HttpEntity<String> request;
        try {
            request = new HttpEntity<>(objectMapper.writeValueAsString(conceptSuggestion), headers);
        } catch (JsonProcessingException e) {
            LOG.error(ERROR_CREATING_A_CONCEPT_IN_TERMINOLOGY_API, e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
        try {
            response = restTemplate.postForObject(createTerminologyConceptSuggestionApiUrl(vocabularyId), request, String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // ok to continue
                response = "[]";
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
            final Set<ConceptSuggestion> results = new HashSet<>();
            final Set<Concept> resultAsConcept = new HashSet<>(); //always just one result in this Set.
            results.add(mapper.readValue(response, new TypeReference<ConceptSuggestion>() {
            }));

            resultAsConcept.add(new Concept(results.iterator().next()));

            wrapper.setResults(new HashSet<>(resultAsConcept));
            return Response.ok(wrapper).build();
        } catch (final Exception e) {
            LOG.error("Error parsing conceptSuggestion from terminology response!", e);
            throw new UnreachableTerminologyApiException(ErrorConstants.ERR_MSG_CANT_REACH_TERMINOLOGY_API);
        }
    }

    private String createTerminologyConceptSuggestionApiUrl(final String vocabularyId) {
        String conceptSuggestionUrl = terminologyProperties.getUrl() + TERMINOLOGY_API_CONCEPT_SUGGESTION_CONTEXT_PATH + "/vocabulary" + "/" + vocabularyId + "/conceptSuggestion";
        LOG.info("Terminology conceptSuggestionUrl created in Codelist TerminologyProxyResource is " + conceptSuggestionUrl);
        return conceptSuggestionUrl;
    }
}
