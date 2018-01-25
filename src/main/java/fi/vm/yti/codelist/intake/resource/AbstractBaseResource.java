package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.jaxrs.cfg.EndpointConfigBase;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterModifier;
import org.slf4j.LoggerFactory;

import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

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

    protected Response handleInternalServerError(final Meta meta, final ResponseWrapper wrapper, final String logMessage, final Exception e) {
        handleLoggingAndMetaForHttpCode(500, meta, logMessage, e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
    }

    protected Response handleInternalServerError(final Meta meta, final MetaResponseWrapper wrapper, final String logMessage, final Exception e) {
        handleLoggingAndMetaForHttpCode(500, meta, logMessage, e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(wrapper).build();
    }

    protected Response handleUnauthorizedAccess(final Meta meta, final ResponseWrapper wrapper, final String logMessage) {
        handleLoggingAndMetaForHttpCode(401, meta, logMessage);
        return Response.status(Response.Status.UNAUTHORIZED).entity(wrapper).build();
    }

    protected Response handleUnauthorizedAccess(final Meta meta, final MetaResponseWrapper wrapper, final String logMessage) {
        handleLoggingAndMetaForHttpCode(401, meta, logMessage);
        return Response.status(Response.Status.UNAUTHORIZED).entity(wrapper).build();
    }

    protected Response handleStartDateLaterThanEndDate(final Meta meta, final ResponseWrapper wrapper) {
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(wrapper).build();
    }

    protected Response handleStartDateLaterThanEndDate(final Meta meta, final MetaResponseWrapper wrapper) {
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(wrapper).build();
    }

    protected boolean startDateIsBeforeEndDateSanityCheck(final Date startDate, final Date endDate) {
        if (startDate == null || endDate == null) {
            return true; // if either one is null, everything is OK
        }
        return startDate.before(endDate) || startAndEndDatesAreOnTheSameDay(startDate, endDate);
    }

    /**
     * This is needed to allow start and end date on the same day - the users might want to enable a code or
     * a codescheme for one day.
     */
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    private boolean startAndEndDatesAreOnTheSameDay(final Date startDate, final Date endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        final Date startDateWithoutTime;
        final Date endDateWithoutTime;
        try {
            startDateWithoutTime = sdf.parse(sdf.format(startDate));
            endDateWithoutTime = sdf.parse(sdf.format(endDate));
        } catch (ParseException e) {
            return true; // should never ever happen, dates are never null here and are coming from datepicker
        }
        return startDateWithoutTime.compareTo(endDate) == 0;
    }

    private void handleLoggingAndMetaForHttpCode(final int code, Meta meta, final String logMessage) {
        LOG.error(logMessage, new WebApplicationException(code));
        meta.setCode(code);
    }

    private void handleLoggingAndMetaForHttpCode(final int code, Meta meta, final String logMessage, final Exception e) {
        LOG.error(logMessage, e);
        meta.setCode(code);
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

    public ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }
}
