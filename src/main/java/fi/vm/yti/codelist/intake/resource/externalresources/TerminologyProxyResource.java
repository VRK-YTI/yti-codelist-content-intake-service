package fi.vm.yti.codelist.intake.resource.externalresources;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.configuration.TerminologyProperties;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.UnreachableTerminologyApiException;
import fi.vm.yti.codelist.intake.model.Meta;
import fi.vm.yti.codelist.intake.resource.AbstractBaseResource;
import fi.vm.yti.codelist.intake.terminology.Concept;
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
    public Response getVocabularities() {
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
        return terminologyProperties.getUrl() + API_PATH_TERMINOLOGY + TERMINOLOGY_API_CONTEXT_PATH + API_PATH_VOCABULARIES;
    }

    @GET
    @Path("/concepts/searchterm/{searchTerm}/vocabulary/{vocabularyId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Returns a filtered list of concepts")
    @ApiResponse(code = 200, message = "Returns success.")
    @SuppressWarnings("Duplicates")
    public Response getConcepts(@PathParam("searchTerm") String searchTerm,
                                @PathParam("vocabularyId") String vocabularyId) {
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        final Meta meta = new Meta();
        final ResponseWrapper<Concept> wrapper = new ResponseWrapper<>(meta);
        String response;
        try {
            response = restTemplate.getForObject(createTerminologyConceptsApiUrl(searchTerm, vocabularyId), String.class);
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

    private String createTerminologyConceptsApiUrl(final String searchTerm,
                                                   final String vocabularyId) {
        return terminologyProperties.getUrl() + API_PATH_TERMINOLOGY + TERMINOLOGY_API_CONTEXT_PATH + API_PATH_CONCEPTS + "/searchterm" + "/" + searchTerm + "/" + "/vocabulary" + "/" + vocabularyId;
    }
}
