package fi.vm.yti.codelist.intake.log;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.dto.Views;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;

@Service
public class EntityPayloadLoggerImpl implements EntityPayloadLogger {

    private static final Logger LOG = LoggerFactory.getLogger(EntityPayloadLoggerImpl.class);
    private static final String CODEREGISTRY = "CodeRegistry";
    private static final String CODESCHEME = "CodeScheme";
    private static final String CODE = "Code";
    private static final String EXTERNALREFERENCE = "ExternalReference";
    private static final String PROPERTYTYPE = "PropertyType";
    private final AuthorizationManager authorizationManager;
    private final Tracer tracer;
    private final ObjectMapper mapper;

    public EntityPayloadLoggerImpl(final AuthorizationManager authorizationManager,
                                   final Tracer tracer) {
        this.authorizationManager = authorizationManager;
        this.tracer = tracer;
        this.mapper = createMapper();
    }

    private ObjectMapper createMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }

    public void logCodeRegistry(final CodeRegistry codeRegistry) {
        beginPayloadLogging(CODEREGISTRY, codeRegistry.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.Normal.class).writeValueAsString(codeRegistry));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for codeRegistry: %s", codeRegistry.getId()), e);
        }
        endPayloadLogging(CODEREGISTRY, codeRegistry.getId());
    }

    public void logCodeScheme(final CodeScheme codeScheme) {
        beginPayloadLogging(CODESCHEME, codeScheme.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.ExtendedCodeScheme.class).writeValueAsString(codeScheme));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for codeScheme: %s", codeScheme.getId()), e);
        }
        endPayloadLogging(CODESCHEME, codeScheme.getId());
    }

    public void logCode(final Code code) {
        beginPayloadLogging(CODE, code.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.ExtendedCode.class).writeValueAsString(code));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for code: %s", code.getId()), e);
        }
        endPayloadLogging(CODE, code.getId());
    }

    public void logExternalReference(final ExternalReference externalReference) {
        beginPayloadLogging(EXTERNALREFERENCE, externalReference.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.ExtendedExternalReference.class).writeValueAsString(externalReference));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for externalReference: %s", externalReference.getId()), e);
        }
        endPayloadLogging(EXTERNALREFERENCE, externalReference.getId());
    }

    public void logPropertyType(final PropertyType propertyType) {
        beginPayloadLogging(PROPERTYTYPE, propertyType.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.Normal.class).writeValueAsString(propertyType));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for propertyType: %s", propertyType.getId()), e);
        }
        endPayloadLogging(PROPERTYTYPE, propertyType.getId());
    }

    private void beginPayloadLogging(final String name,
                                     final UUID identifier) {
        LOG.debug(String.format("*** Begin %s payload logging for ID: %s with TraceId: %s and UserId: %s ***", name, identifier, getTraceId(), getUserId()));
    }

    private void endPayloadLogging(final String name,
                                   final UUID identifier) {
        LOG.debug(String.format("*** End %s payload logging for ID: %s ***", name, identifier.toString()));
    }

    private String getTraceId() {
        final Span span = tracer.getCurrentSpan();
        if (span != null) {
            return span.traceIdString();
        }
        return null;
    }

    private String getUserId() {
        final UUID userId = authorizationManager.getUserId();
        if (userId != null) {
            return userId.toString();
        }
        return null;
    }
}
