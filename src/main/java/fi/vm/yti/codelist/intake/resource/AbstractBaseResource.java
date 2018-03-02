package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier;

import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FIELD_NAME_ID;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FIELD_NAME_URI;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODEREGISTRY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODESCHEME;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_DATACLASSIFICATION;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_EXTERNALREFERENCE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_ORGANIZATION;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_PROPERTYTYPE;

public abstract class AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseResource.class);

    public SimpleFilterProvider createSimpleFilterProvider(final String baseFilter,
                                                           final String expand) {
        final List<String> baseFilters = new ArrayList<>();
        baseFilters.add(baseFilter);
        return createSimpleFilterProvider(baseFilters, expand);
    }

    public SimpleFilterProvider createSimpleFilterProvider(final List<String> baseFilters,
                                                           final String expand) {
        final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(FILTER_NAME_CODEREGISTRY, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI));
        filterProvider.addFilter(FILTER_NAME_CODESCHEME, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI));
        filterProvider.addFilter(FILTER_NAME_CODE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI));
        filterProvider.addFilter(FILTER_NAME_EXTERNALREFERENCE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI));
        filterProvider.addFilter(FILTER_NAME_PROPERTYTYPE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI));
        filterProvider.addFilter(FILTER_NAME_DATACLASSIFICATION, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI));
        filterProvider.addFilter(FILTER_NAME_ORGANIZATION, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_ID));
        filterProvider.setFailOnUnknownId(false);
        for (final String baseFilter : baseFilters) {
            filterProvider.removeFilter(baseFilter);
        }
        if (expand != null && !expand.isEmpty()) {
            final List<String> filterOptions = Arrays.asList(expand.split(","));
            for (final String filter : filterOptions) {
                filterProvider.removeFilter(filter);
            }
        }
        return filterProvider;
    }

    public void logApiRequest(final Logger logger,
                              final String method,
                              final String apiVersionPath,
                              final String apiPath) {
        logger.info(method + " " + apiVersionPath + apiPath + " requested!");
    }

    protected Response handleInternalServerError(final Meta meta,
                                                 final ResponseWrapper wrapper,
                                                 final String logMessage,
                                                 final Exception e,
                                                 final String msgToUser) {
        handleLoggingAndMetaForHttpCode(500, meta, logMessage, e, Optional.of(msgToUser));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
    }

    protected Response handleInternalServerError(final Meta meta,
                                                 final MetaResponseWrapper wrapper,
                                                 final String logMessage,
                                                 final Exception e,
                                                 final String msgToUser) {
        handleLoggingAndMetaForHttpCode(500, meta, logMessage, e, Optional.of(msgToUser));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
    }

    protected Response handleUnauthorizedAccess(final Meta meta,
                                                final ResponseWrapper wrapper,
                                                final String logMessage) {
        handleLoggingAndMetaForHttpCode(401, meta, logMessage, Optional.of(logMessage));
        return Response.status(Response.Status.UNAUTHORIZED).entity(wrapper).build();
    }

    protected Response handleUnauthorizedAccess(final Meta meta,
                                                final MetaResponseWrapper wrapper,
                                                final String logMessage) {
        handleLoggingAndMetaForHttpCode(401, meta, logMessage, Optional.of(logMessage));
        return Response.status(Response.Status.UNAUTHORIZED).entity(wrapper).build();
    }

    private void handleLoggingAndMetaForHttpCode(final int code,
                                                 Meta meta,
                                                 final String logMessage,
                                                 Optional<String> msgToUser) {
        LOG.error(logMessage, new WebApplicationException(code));
        meta.setCode(code);
        if (msgToUser.isPresent()) {
            meta.setMessage(msgToUser.get());
        }
    }

    private void handleLoggingAndMetaForHttpCode(final int code,
                                                 Meta meta,
                                                 final String logMessage,
                                                 final Exception e,
                                                 Optional<String> msgToUser) {
        LOG.error(logMessage, e);
        meta.setCode(code);
        if (msgToUser.isPresent()) {
            meta.setMessage(msgToUser.get());
        }
    }

    public ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }

    static class FilterModifier extends ObjectWriterModifier {

        private final FilterProvider provider;

        protected FilterModifier(final FilterProvider provider) {
            this.provider = provider;
        }

        @Override
        public ObjectWriter modify(final EndpointConfigBase<?> endpoint,
                                   final MultivaluedMap<String, Object> responseHeaders,
                                   final Object valueToWrite,
                                   final ObjectWriter w,
                                   final JsonGenerator g) throws IOException {
            return w.with(provider);
        }
    }
}
