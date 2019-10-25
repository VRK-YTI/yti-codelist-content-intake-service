package fi.vm.yti.codelist.intake.resource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.EndpointConfigBase;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterModifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

public interface AbstractBaseResource {

    default SimpleFilterProvider createSimpleFilterProvider(final String baseFilter,
                                                            final String expand) {
        final List<String> baseFilters = new ArrayList<>();
        baseFilters.add(baseFilter);
        return createSimpleFilterProvider(baseFilters, expand);
    }

    default SimpleFilterProvider createBaseFilterProvider() {
        final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(FILTER_NAME_CODEREGISTRY, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_CODESCHEME, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_CODE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_EXTERNALREFERENCE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_PROPERTYTYPE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_INFODOMAIN, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_ORGANIZATION, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_ID));
        filterProvider.addFilter(FILTER_NAME_EXTENSION, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_MEMBER, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_VALUETYPE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_MEMBERVALUE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_ID));
        filterProvider.addFilter(FILTER_NAME_SEARCHHIT, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI));
        return filterProvider;
    }

    default SimpleFilterProvider createSimpleFilterProvider(final List<String> baseFilters,
                                                            final String expand) {
        final SimpleFilterProvider filterProvider = createBaseFilterProvider();
        filterProvider.setFailOnUnknownId(false);
        for (final String baseFilter : baseFilters) {
            filterProvider.removeFilter(baseFilter.trim());
        }
        if (expand != null && !expand.isEmpty()) {
            final String[] filterOptions = expand.split(",");
            for (final String filter : filterOptions) {
                filterProvider.removeFilter(filter.trim());
            }
        }
        return filterProvider;
    }

    default ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    class FilterModifier extends ObjectWriterModifier {
        private final FilterProvider provider;
        private final boolean pretty;

        FilterModifier(final FilterProvider provider,
                       final String pretty) {
            this.provider = provider;
            this.pretty = pretty != null;
        }

        @Override
        public ObjectWriter modify(final EndpointConfigBase<?> endpoint,
                                   final MultivaluedMap<String, Object> responseHeaders,
                                   final Object valueToWrite,
                                   final ObjectWriter writer,
                                   final JsonGenerator jsonGenerator) {
            if (pretty) {
                jsonGenerator.useDefaultPrettyPrinter();
            }
            return writer.with(provider);
        }
    }
}
