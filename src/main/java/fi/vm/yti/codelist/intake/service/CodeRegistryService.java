package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class CodeRegistryService extends BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryService.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRegistryParser codeRegistryParser;
    private final OrganizationRepository organizationRepository;
    private final ApiUtils apiUtils;

    @Inject
    public CodeRegistryService(final AuthorizationManager authorizationManager,
                               final CodeRegistryRepository codeRegistryRepository,
                               final CodeRegistryParser codeRegistryParser,
                               final OrganizationRepository organizationRepository,
                               final ApiUtils apiUtils) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeRegistryParser = codeRegistryParser;
        this.organizationRepository = organizationRepository;
        this.apiUtils = apiUtils;
    }

    @Transactional
    public Set<CodeRegistryDTO> findAll() {
        return mapCodeRegistryDtos(codeRegistryRepository.findAll());
    }

    @Transactional
    @Nullable
    public CodeRegistryDTO findByCodeValue(final String codeValue) {
        CodeRegistry registry = codeRegistryRepository.findByCodeValue(codeValue);
        if (registry == null)
            return null;
        return mapCodeRegistryDto(registry);
    }

    @Transactional
    public Set<CodeRegistryDTO> parseAndPersistCodeRegistriesFromSourceData(final String format,
                                                                            final InputStream inputStream,
                                                                            final String jsonPayload) {
        Set<CodeRegistry> codeRegistries = new HashSet<>();
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeRegistries = updateCodeRegistryEntities(codeRegistryParser.parseCodeRegistriesFromJsonData(jsonPayload));
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                codeRegistries = updateCodeRegistryEntities(codeRegistryParser.parseCodeRegistriesFromExcelInputStream(inputStream));
                break;
            case FORMAT_CSV:
                codeRegistries = updateCodeRegistryEntities(codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream));
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapCodeRegistryDtos(codeRegistries);
    }

    private CodeRegistry updateCodeRegistryEntity(final CodeRegistryDTO codeRegistryDto) {
        final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(codeRegistryDto);
        if (codeRegistry != null) {
            codeRegistryRepository.save(codeRegistry);
        }
        return codeRegistry;
    }

    private Set<CodeRegistry> updateCodeRegistryEntities(final Set<CodeRegistryDTO> codeRegistryDtos) {
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        codeRegistryDtos.forEach(codeRegistry -> codeRegistries.add(createOrUpdateCodeRegistry(codeRegistry)));
        if (codeRegistries != null && !codeRegistries.isEmpty()) {
            codeRegistryRepository.save(codeRegistries);
        }
        return codeRegistries;
    }

    @Transactional
    public CodeRegistryDTO parseAndPersistCodeRegistryFromJson(final String codeRegistryCodeValue,
                                                               final String jsonPayload) {
        final CodeRegistry existingCodeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        final CodeRegistry codeRegistry;
        if (existingCodeRegistry != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final CodeRegistryDTO codeRegistryDto = codeRegistryParser.parseCodeRegistryFromJsonData(jsonPayload);
                    if (!codeRegistryDto.getCodeValue().equalsIgnoreCase(codeRegistryCodeValue)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    codeRegistry = updateCodeRegistryEntity(codeRegistryDto);
                    codeRegistryRepository.save(codeRegistry);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistCodeRegistryFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return mapCodeRegistryDto(codeRegistry);
    }

    private CodeRegistry createOrUpdateCodeRegistry(final CodeRegistryDTO fromCodeRegistry) {
        final CodeRegistry codeRegistry;
        final CodeRegistry existingCodeRegistry = codeRegistryRepository.findByCodeValue(fromCodeRegistry.getCodeValue());
        if (existingCodeRegistry != null) {
            codeRegistry = updateCodeRegistry(existingCodeRegistry, fromCodeRegistry);
        } else {
            codeRegistry = createCodeRegistry(fromCodeRegistry);
        }
        return codeRegistry;
    }

    private CodeRegistry updateCodeRegistry(final CodeRegistry codeRegistry,
                                            final CodeRegistryDTO fromCodeRegistry) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final String uri = apiUtils.createCodeRegistryUri(codeRegistry);
        final String url = apiUtils.createCodeRegistryUrl(codeRegistry);
        boolean hasChanges = false;
        if (!Objects.equals(codeRegistry.getUri(), uri)) {
            codeRegistry.setUri(uri);
            hasChanges = true;
        }
        if (!Objects.equals(codeRegistry.getUrl(), url)) {
            codeRegistry.setUrl(url);
            hasChanges = true;
        }
        codeRegistry.setOrganizations(resolveOrganizationsFromDtos(fromCodeRegistry.getOrganizations()));
        for (final Map.Entry<String, String> entry : fromCodeRegistry.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(codeRegistry.getPrefLabel(language), value)) {
                codeRegistry.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeRegistry.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(codeRegistry.getDefinition(language), value)) {
                codeRegistry.setDefinition(language, value);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            codeRegistry.setModified(timeStamp);
        }
        return codeRegistry;
    }

    private Set<Organization> resolveOrganizationsFromDtos(final Set<OrganizationDTO> organizationDtos) {
        final Set<Organization> organizations = new HashSet<>();
        organizationDtos.forEach(organizationDto -> organizations.add(organizationRepository.findById(organizationDto.getId())));
        return organizations;
    }

    private CodeRegistry createCodeRegistry(final CodeRegistryDTO fromCodeRegistry) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final CodeRegistry codeRegistry = new CodeRegistry();
        codeRegistry.setId(UUID.randomUUID());
        codeRegistry.setCodeValue(fromCodeRegistry.getCodeValue());
        codeRegistry.setModified(timeStamp);
        codeRegistry.setOrganizations(resolveOrganizationsFromDtos(fromCodeRegistry.getOrganizations()));
        for (Map.Entry<String, String> entry : fromCodeRegistry.getPrefLabel().entrySet()) {
            codeRegistry.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCodeRegistry.getDefinition().entrySet()) {
            codeRegistry.setDefinition(entry.getKey(), entry.getValue());
        }
        codeRegistry.setUri(apiUtils.createCodeRegistryUri(codeRegistry));
        codeRegistry.setUrl(apiUtils.createCodeRegistryUrl(codeRegistry));
        return codeRegistry;
    }
}
