package fi.vm.yti.codelist.intake.log;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import brave.Span;
import brave.Tracer;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.Views;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.model.ValueType;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.impl.DtoMapperService;

@Service
public class EntityPayloadLoggerImpl implements EntityPayloadLogger {

    private static final Logger LOG = LoggerFactory.getLogger(EntityPayloadLoggerImpl.class);
    private static final String CODEREGISTRY = "CodeRegistry";
    private static final String CODESCHEME = "CodeScheme";
    private static final String CODE = "Code";
    private static final String EXTERNALREFERENCE = "ExternalReference";
    private static final String PROPERTYTYPE = "PropertyType";
    private static final String EXTENSION = "Extension";
    private static final String MEMBER = "Member";
    private static final String VALUETYPE = "ValueType";

    private final AuthorizationManager authorizationManager;
    private final Tracer tracer;
    private final ObjectMapper mapper;
    private final DtoMapperService dtoMapperService;

    @Inject
    public EntityPayloadLoggerImpl(final AuthorizationManager authorizationManager,
                                   final Tracer tracer,
                                   final DtoMapperService dtoMapperService) {
        this.authorizationManager = authorizationManager;
        this.tracer = tracer;
        this.dtoMapperService = dtoMapperService;
        this.mapper = createMapper();
    }

    private ObjectMapper createMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return objectMapper;
    }

    @Transactional
    public void logCodeRegistry(final CodeRegistry codeRegistry) {
        beginPayloadLogging(CODEREGISTRY, codeRegistry.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.Normal.class).writeValueAsString(dtoMapperService.mapDeepCodeRegistryDto(codeRegistry)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for codeRegistry: %s", codeRegistry.getId()), e);
        }
        endPayloadLogging(CODEREGISTRY, codeRegistry.getId());
    }

    @Transactional
    public void logCodeScheme(final CodeScheme codeScheme) {
        beginPayloadLogging(CODESCHEME, codeScheme.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.ExtendedCodeScheme.class).writeValueAsString(dtoMapperService.mapDeepCodeSchemeDto(codeScheme)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for codeScheme: %s", codeScheme.getId()), e);
        }
        endPayloadLogging(CODESCHEME, codeScheme.getId());
    }

    @Transactional
    public void logCode(final Code code) {
        beginPayloadLogging(CODE, code.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.ExtendedCode.class).writeValueAsString(dtoMapperService.mapDeepCodeDto(code)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for code: %s", code.getId()), e);
        }
        endPayloadLogging(CODE, code.getId());
    }

    @Transactional
    public void logExternalReference(final ExternalReference externalReference) {
        beginPayloadLogging(EXTERNALREFERENCE, externalReference.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.ExtendedExternalReference.class).writeValueAsString(dtoMapperService.mapDeepExternalReferenceDto(externalReference)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for externalReference: %s", externalReference.getId()), e);
        } catch (final Exception e) {
            LOG.error("Exception caught when logging externalReference: ", e);
        }
        endPayloadLogging(EXTERNALREFERENCE, externalReference.getId());
    }

    @Transactional
    public void logPropertyType(final PropertyType propertyType) {
        beginPayloadLogging(PROPERTYTYPE, propertyType.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.Normal.class).writeValueAsString(dtoMapperService.mapPropertyTypeDto(propertyType)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for propertyType: %s", propertyType.getId()), e);
        }
        endPayloadLogging(PROPERTYTYPE, propertyType.getId());
    }

    @Transactional
    public void logExtension(final Extension extension) {
        beginPayloadLogging(EXTENSION, extension.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.Normal.class).writeValueAsString(dtoMapperService.mapExtensionDto(extension)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for extension: %s", extension.getId()), e);
        }
        endPayloadLogging(EXTENSION, extension.getId());
    }

    @Transactional
    public void logMember(final Member member) {
        beginPayloadLogging(MEMBER, member.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.Normal.class).writeValueAsString(dtoMapperService.mapMemberDto(member)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for member: %s", member.getId()), e);
        }
        endPayloadLogging(MEMBER, member.getId());
    }

    @Transactional
    public void logMembers(final Set<Member> members) {
        members.forEach(this::logMember);
    }

    @Transactional
    public void logValueType(final ValueType valueType) {
        beginPayloadLogging(VALUETYPE, valueType.getId());
        try {
            LOG.debug(mapper.writerWithView(Views.Normal.class).writeValueAsString(dtoMapperService.mapValueTypeDto(valueType)));
        } catch (final JsonProcessingException e) {
            LOG.error(String.format("Failed to write log for valueType: %s", valueType.getId()), e);
        }
        endPayloadLogging(VALUETYPE, valueType.getId());
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
        final Span span = tracer.currentSpan();
        if (span != null) {
            return span.context().traceIdString();
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
