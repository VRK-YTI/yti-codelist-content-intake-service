package fi.vm.yti.codelist.intake.resource.externalresources;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.terminology.Concept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.configuration.TerminologyProperties;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.model.Meta;
import fi.vm.yti.codelist.intake.resource.AbstractBaseResource;
import fi.vm.yti.codelist.intake.terminology.Vocabulary;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_401;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_500;

@Component
@Path("/v1/terminology")
@Api(value = "terminology")
public class TerminologyProxyResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(TerminologyProxyResource.class);
    private final RestTemplate restTemplate;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private TerminologyProperties terminologyProperties;

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
        logApiRequest(LOG, METHOD_GET, API_PATH_VERSION_V1, API_PATH_TERMINOLOGY + API_PATH_VOCABULARIES);
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        final String response = restTemplate.getForObject(createTerminologyVocabulariesApiUrl(), String.class);
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
            LOG.error("Error parsing vocabularies from terminology response! ", e);
            meta.setMessage("Error parsing vocabularies from terminology!");
            meta.setCode(500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
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
        logApiRequest(LOG, METHOD_GET, API_PATH_VERSION_V1, API_PATH_TERMINOLOGY + API_PATH_CONCEPTS);
        final YtiUser user = authenticatedUserProvider.getUser();
        if (user.isAnonymous()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        final Meta meta = new Meta();
        final ResponseWrapper<Concept> wrapper = new ResponseWrapper<>(meta);
        String response = "";
        try {
            response = restTemplate.getForObject(createTerminologyConceptsApiUrl(searchTerm, vocabularyId), String.class);
        } catch (Exception e) {
            LOG.error("Error getting concepts from terminology-api! ", e);
            meta.setMessage("Error parsing concepts from terminology!");
            meta.setCode(500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
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
            LOG.error("Error parsing concepts from terminology response! ", e);
            meta.setMessage("Error parsing concepts from terminology!");
            meta.setCode(500);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
        }
    }

    private String createTerminologyConceptsApiUrl(String searchTerm, String vocabularyId) {
        return terminologyProperties.getUrl() + API_PATH_TERMINOLOGY + TERMINOLOGY_API_CONTEXT_PATH + API_PATH_CONCEPTS + "/searchterm" + "/" + searchTerm + "/" + "/vocabulary" + "/" + vocabularyId;
    }
}
