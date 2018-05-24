package fi.vm.yti.codelist.intake.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.impl.IndexingImpl;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.model.PropertyType;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_500;

abstract class BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingImpl.class);
    private static final String SORT_ASC = "ASC";
    private static final String SORT_DESC = "DESC";
    private static final String ENTITY_CODEREGISTRY = "extension";
    private static final String ENTITY_CODESCHEME = "codescheme";
    private static final String ENTITY_CODE = "code";
    private static final String ENTITY_EXTENSIONSCHEME = "extensionscheme";
    private static final String ENTITY_EXTENSION = "extension";
    private static final String ENTITY_EXTERNALREFERENCE = "externalreference";
    private static final String ENTITY_PROPERTYTYPE = "propertytype";

    private final ApiUtils apiUtils;
    private final DataSource dataSource;

    public BaseService(final ApiUtils apiUtils,
                       final DataSource dataSource) {
        this.apiUtils = apiUtils;
        this.dataSource = dataSource;
    }

    public CodeDTO mapDeepCodeDto(final Code code) {
        return mapCodeDto(code, true);
    }

    public CodeDTO mapCodeDto(final Code code,
                              final boolean deep) {
        final CodeDTO codeDto = new CodeDTO();
        codeDto.setId(code.getId());
        codeDto.setCodeValue(code.getCodeValue());
        codeDto.setUri(code.getUri());
        codeDto.setBroaderCodeId(code.getBroaderCodeId());
        codeDto.setStartDate(code.getStartDate());
        codeDto.setEndDate(code.getEndDate());
        codeDto.setStatus(code.getStatus());
        codeDto.setHierarchyLevel(code.getHierarchyLevel());
        codeDto.setShortName(code.getShortName());
        codeDto.setPrefLabel(code.getPrefLabel());
        codeDto.setDefinition(code.getDefinition());
        codeDto.setCodeScheme(mapCodeSchemeDto(code.getCodeScheme(), false));
        codeDto.setConceptUriInVocabularies(code.getConceptUriInVocabularies());
        if (deep) {
            if (code.getExternalReferences() != null) {
                codeDto.setExternalReferences(mapExternalReferenceDtos(code.getExternalReferences(), false));
            }
            if (code.getExtensions() != null) {
                codeDto.setExtensions(mapExtensionDtos(code.getExtensions(), false));
            }
        }
        codeDto.setDescription(code.getDescription());
        codeDto.setOrder(code.getOrder());
        codeDto.setUrl(apiUtils.createCodeUrl(codeDto));
        codeDto.setCreated(getFirstModificationDate(ENTITY_CODE, code.getId().toString()));
        codeDto.setModified(getLastModificationDate(ENTITY_CODE, code.getId().toString()));
        return codeDto;
    }

    public Set<CodeDTO> mapDeepCodeDtos(final Set<Code> codes) {
        return mapCodeDtos(codes, true);
    }

    public Set<CodeDTO> mapCodeDtos(final Set<Code> codes,
                                    final boolean deep) {
        final Set<CodeDTO> codeDtos = new HashSet<>();
        if (codes != null) {
            for (final Code code : codes) {
                codeDtos.add(mapCodeDto(code, deep));
            }
        }
        return codeDtos;
    }

    public CodeSchemeDTO mapDeepCodeSchemeDto(final CodeScheme codeScheme) {
        return mapCodeSchemeDto(codeScheme, true);
    }

    public CodeSchemeDTO mapCodeSchemeDto(final CodeScheme codeScheme,
                                          final boolean deep) {
        final CodeSchemeDTO codeSchemeDto = new CodeSchemeDTO();
        codeSchemeDto.setId(codeScheme.getId());
        codeSchemeDto.setCodeValue(codeScheme.getCodeValue());
        codeSchemeDto.setUri(codeScheme.getUri());
        codeSchemeDto.setChangeNote(codeScheme.getChangeNote());
        codeSchemeDto.setStartDate(codeScheme.getStartDate());
        codeSchemeDto.setEndDate(codeScheme.getEndDate());
        codeSchemeDto.setStatus(codeScheme.getStatus());
        codeSchemeDto.setDefinition(codeScheme.getDefinition());
        codeSchemeDto.setPrefLabel(codeScheme.getPrefLabel());
        codeSchemeDto.setDescription(codeScheme.getDescription());
        codeSchemeDto.setCodeRegistry(mapCodeRegistryDto(codeScheme.getCodeRegistry()));
        codeSchemeDto.setVersion(codeScheme.getVersion());
        codeSchemeDto.setGovernancePolicy(codeScheme.getGovernancePolicy());
        codeSchemeDto.setLegalBase(codeScheme.getLegalBase());
        codeSchemeDto.setConceptUriInVocabularies(codeScheme.getConceptUriInVocabularies());
        if (deep) {
            if (codeScheme.getDataClassifications() != null) {
                codeSchemeDto.setDataClassifications(mapCodeDtos(codeScheme.getDataClassifications(), false));
            }
            if (codeScheme.getExternalReferences() != null) {
                codeSchemeDto.setExternalReferences(mapExternalReferenceDtos(codeScheme.getExternalReferences(), false));
            }
            if (codeScheme.getExtensionSchemes() != null) {
                codeSchemeDto.setExtensionSchemes(mapExtensionSchemeDtos(codeScheme.getExtensionSchemes(), false));
            }
        }
        codeSchemeDto.setUrl(apiUtils.createCodeSchemeUrl(codeSchemeDto));
        codeSchemeDto.setCreated(getFirstModificationDate(ENTITY_CODESCHEME, codeScheme.getId().toString()));
        codeSchemeDto.setModified(getLastModificationDate(ENTITY_CODESCHEME, codeScheme.getId().toString()));
        return codeSchemeDto;
    }

    public Set<CodeSchemeDTO> mapDeepCodeSchemeDtos(final Set<CodeScheme> codeSchemes) {
        return mapCodeSchemeDtos(codeSchemes, true);
    }

    public Set<CodeSchemeDTO> mapCodeSchemeDtos(final Set<CodeScheme> codeSchemes,
                                                final boolean deep) {
        final Set<CodeSchemeDTO> codeSchemeDTOS = new HashSet<>();
        if (codeSchemes != null) {
            for (final CodeScheme codeScheme : codeSchemes) {
                codeSchemeDTOS.add(mapCodeSchemeDto(codeScheme, deep));
            }
        }
        return codeSchemeDTOS;
    }

    public CodeRegistryDTO mapCodeRegistryDto(final CodeRegistry codeRegistry) {
        final CodeRegistryDTO codeRegistryDto = new CodeRegistryDTO();
        codeRegistryDto.setId(codeRegistry.getId());
        codeRegistryDto.setCodeValue(codeRegistry.getCodeValue());
        codeRegistryDto.setUri(codeRegistry.getUri());
        codeRegistryDto.setPrefLabel(codeRegistry.getPrefLabel());
        codeRegistryDto.setDefinition(codeRegistry.getDefinition());
        codeRegistryDto.setOrganizations(mapOrganizationDtos(codeRegistry.getOrganizations(), false));
        codeRegistryDto.setUrl(apiUtils.createCodeRegistryUrl(codeRegistryDto));
        codeRegistryDto.setCreated(getFirstModificationDate(ENTITY_CODEREGISTRY, codeRegistry.getId().toString()));
        codeRegistryDto.setModified(getLastModificationDate(ENTITY_CODEREGISTRY, codeRegistry.getId().toString()));
        return codeRegistryDto;
    }

    public Set<CodeRegistryDTO> mapCodeRegistryDtos(final Set<CodeRegistry> codeRegistries) {
        final Set<CodeRegistryDTO> codeRegistryDtos = new HashSet<>();
        if (codeRegistries != null) {
            for (final CodeRegistry codeRegistry : codeRegistries) {
                codeRegistryDtos.add(mapCodeRegistryDto(codeRegistry));
            }
        }
        return codeRegistryDtos;
    }

    public ExternalReferenceDTO mapDeepExternalReferenceDto(final ExternalReference externalReference) {
        return mapExternalReferenceDto(externalReference, true);
    }

    public ExternalReferenceDTO mapExternalReferenceDto(final ExternalReference externalReference,
                                                        final boolean deep) {
        final ExternalReferenceDTO externalReferenceDto = new ExternalReferenceDTO();
        externalReferenceDto.setId(externalReference.getId());
        externalReferenceDto.setDescription(externalReference.getDescription());
        externalReferenceDto.setGlobal(externalReference.getGlobal());
        externalReferenceDto.setTitle(externalReference.getTitle());
        externalReferenceDto.setHref(externalReference.getHref());
        externalReferenceDto.setPropertyType(mapPropertyTypeDto(externalReference.getPropertyType()));
        if (externalReference.getParentCodeScheme() != null) {
            externalReferenceDto.setParentCodeScheme(mapCodeSchemeDto(externalReference.getParentCodeScheme(), false));
        }
        if (deep) {
            if (externalReference.getCodeSchemes() != null) {
                externalReferenceDto.setCodeSchemes(mapCodeSchemeDtos(externalReference.getCodeSchemes(), false));
            }
            if (externalReference.getCodes() != null) {
                externalReferenceDto.setCodes(mapCodeDtos(externalReference.getCodes(), false));
            }
        }
        externalReferenceDto.setUrl(apiUtils.createExternalReferenceUrl(externalReferenceDto));
        externalReferenceDto.setCreated(getFirstModificationDate(ENTITY_EXTERNALREFERENCE, externalReference.getId().toString()));
        externalReferenceDto.setModified(getLastModificationDate(ENTITY_EXTERNALREFERENCE, externalReference.getId().toString()));
        return externalReferenceDto;
    }

    public Set<ExternalReferenceDTO> mapDeepExternalReferenceDtos(final Set<ExternalReference> externalReferences) {
        return mapExternalReferenceDtos(externalReferences, true);
    }

    public Set<ExternalReferenceDTO> mapExternalReferenceDtos(final Set<ExternalReference> externalReferences,
                                                              final boolean deep) {
        final Set<ExternalReferenceDTO> externalReferenceDtos = new HashSet<>();
        if (externalReferences != null) {
            for (final ExternalReference externalReference : externalReferences) {
                externalReferenceDtos.add(mapExternalReferenceDto(externalReference, deep));
            }
        }
        return externalReferenceDtos;
    }

    public PropertyTypeDTO mapPropertyTypeDto(final PropertyType propertyType) {
        final PropertyTypeDTO propertyTypeDto = new PropertyTypeDTO();
        propertyTypeDto.setId(propertyType.getId());
        propertyTypeDto.setContext(propertyType.getContext());
        propertyTypeDto.setDefinition(propertyType.getDefinition());
        propertyTypeDto.setLocalName(propertyType.getLocalName());
        propertyTypeDto.setPrefLabel(propertyType.getPrefLabel());
        propertyTypeDto.setType(propertyType.getType());
        propertyTypeDto.setPropertyUri(propertyType.getPropertyUri());
        propertyTypeDto.setUrl(apiUtils.createPropertyTypeUrl(propertyTypeDto));
        propertyTypeDto.setCreated(getFirstModificationDate(ENTITY_PROPERTYTYPE, propertyType.getId().toString()));
        propertyTypeDto.setModified(getLastModificationDate(ENTITY_PROPERTYTYPE, propertyType.getId().toString()));
        return propertyTypeDto;
    }

    public Set<PropertyTypeDTO> mapPropertyTypeDtos(final Set<PropertyType> propertyTypes) {
        final Set<PropertyTypeDTO> propertyTypeDtos = new HashSet<>();
        if (propertyTypes != null) {
            for (final PropertyType propertyType : propertyTypes) {
                propertyTypeDtos.add(mapPropertyTypeDto(propertyType));
            }
        }
        return propertyTypeDtos;
    }

    public ExtensionDTO mapDeepExtensionDto(final Extension extension) {
        return mapExtensionDto(extension, true);
    }

    public ExtensionDTO mapExtensionDto(final Extension extension,
                                        final boolean deep) {
        final ExtensionDTO extensionDto = new ExtensionDTO();
        extensionDto.setId(extension.getId());
        extensionDto.setOrder(extension.getOrder());
        extensionDto.setExtensionValue(extension.getExtensionValue());
        extensionDto.setCode(mapCodeDto(extension.getCode(), false));
        if (deep) {
            if (extension.getExtension() != null) {
                extensionDto.setExtension(mapExtensionDto(extension.getExtension(), false));
            }
            if (extension.getExtensionScheme() != null) {
                extensionDto.setExtensionScheme(mapExtensionSchemeDto(extension.getExtensionScheme(), false));
            }
        }
        extensionDto.setUrl(apiUtils.createExtensionUrl(extensionDto));
        extensionDto.setCreated(getFirstModificationDate(ENTITY_EXTENSION, extension.getId().toString()));
        extensionDto.setModified(getLastModificationDate(ENTITY_EXTENSION, extension.getId().toString()));
        return extensionDto;
    }

    public Set<ExtensionDTO> mapDeepExtensionDtos(final Set<Extension> extensions) {
        return mapExtensionDtos(extensions, true);
    }

    public Set<ExtensionDTO> mapExtensionDtos(final Set<Extension> extensions,
                                              final boolean deep) {
        final Set<ExtensionDTO> extensionDtos = new HashSet<>();
        if (extensions != null) {
            for (final Extension extension : extensions) {
                extensionDtos.add(mapExtensionDto(extension, deep));
            }
        }
        return extensionDtos;
    }

    public ExtensionSchemeDTO mapDeepExtensionSchemeDto(final ExtensionScheme extensionScheme) {
        return mapExtensionSchemeDto(extensionScheme, true);
    }

    public ExtensionSchemeDTO mapExtensionSchemeDto(final ExtensionScheme extensionScheme,
                                                    final boolean deep) {
        final ExtensionSchemeDTO extensionSchemeDto = new ExtensionSchemeDTO();
        extensionSchemeDto.setId(extensionScheme.getId());
        extensionSchemeDto.setPropertyType(mapPropertyTypeDto(extensionScheme.getPropertyType()));
        extensionSchemeDto.setPrefLabel(extensionScheme.getPrefLabel());
        extensionSchemeDto.setStatus(extensionScheme.getStatus());
        extensionSchemeDto.setCodeValue(extensionScheme.getCodeValue());
        extensionSchemeDto.setStartDate(extensionScheme.getStartDate());
        extensionSchemeDto.setEndDate(extensionScheme.getEndDate());
        if (deep) {
            if (extensionScheme.getParentCodeScheme() != null) {
                extensionSchemeDto.setParentCodeScheme(mapCodeSchemeDto(extensionScheme.getParentCodeScheme(), false));
            }
            if (extensionScheme.getCodeSchemes() != null) {
                extensionSchemeDto.setCodeSchemes(mapCodeSchemeDtos(extensionScheme.getCodeSchemes(), false));
            }
            if (extensionScheme.getExtensions() != null) {
                extensionSchemeDto.setExtensions(mapExtensionDtos(extensionScheme.getExtensions(), false));
            }
        }
        extensionSchemeDto.setUrl(apiUtils.createExtensionSchemeUrl(extensionSchemeDto));
        extensionSchemeDto.setCreated(getFirstModificationDate(ENTITY_EXTENSIONSCHEME, extensionScheme.getId().toString()));
        extensionSchemeDto.setModified(getLastModificationDate(ENTITY_EXTENSIONSCHEME, extensionScheme.getId().toString()));
        return extensionSchemeDto;
    }

    public Set<ExtensionSchemeDTO> mapDeepExtensionSchemeDtos(final Set<ExtensionScheme> extensionSchemes) {
        return mapExtensionSchemeDtos(extensionSchemes, true);
    }

    public Set<ExtensionSchemeDTO> mapExtensionSchemeDtos(final Set<ExtensionScheme> extensionSchemes,
                                                          final boolean deep) {
        final Set<ExtensionSchemeDTO> extensionSchemeDtos = new HashSet<>();
        if (extensionSchemes != null) {
            for (final ExtensionScheme extensionScheme : extensionSchemes) {
                extensionSchemeDtos.add(mapExtensionSchemeDto(extensionScheme, deep));
            }
        }
        return extensionSchemeDtos;
    }

    public OrganizationDTO mapOrganizationDto(final Organization organization,
                                              final boolean deep) {
        final OrganizationDTO organizationDto = new OrganizationDTO();
        organizationDto.setId(organization.getId());
        organizationDto.setRemoved(organization.getRemoved());
        organizationDto.setUrl(organization.getUrl());
        organizationDto.setDescription(organization.getDescription());
        organizationDto.setPrefLabel(organization.getPrefLabel());
        if (deep) {
            if (organization.getCodeRegistries() != null) {
                organizationDto.setCodeRegistries(mapCodeRegistryDtos(organization.getCodeRegistries()));
            }
        }
        return organizationDto;
    }

    public Set<OrganizationDTO> mapOrganizationDtos(final Set<Organization> organizations,
                                                    final boolean deep) {
        final Set<OrganizationDTO> organizationDtos = new HashSet<>();
        if (organizations != null) {
            for (final Organization organization : organizations) {
                organizationDtos.add(mapOrganizationDto(organization, deep));
            }
        }
        return organizationDtos;
    }

    private Date getFirstModificationDate(final String entityName,
                                          final String entityId) {
        return getModificationDate(entityName, entityId, SORT_ASC);
    }

    private Date getLastModificationDate(final String entityName,
                                         final String entityId) {
        return getModificationDate(entityName, entityId, SORT_DESC);
    }

    private Date getModificationDate(final String entityName,
                                     final String entityId,
                                     final String sort) {
        Date modified = null;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(String.format("SELECT c.modified FROM commit as c WHERE c.id IN (SELECT e.commit_id FROM editedentity AS e WHERE e.%s_id = '%s') ORDER BY c.modified " + sort + " LIMIT 1;", entityName, entityId));
             final ResultSet results = ps.executeQuery()) {
            if (results.next()) {
                modified = results.getTimestamp(1);
            }
        } catch (final SQLException e) {
            LOG.error("SQL query failed: ", e);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return modified;
    }
}
