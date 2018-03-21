package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.parser.PropertyTypeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class PropertyTypeService extends BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyTypeService.class);
    private final AuthorizationManager authorizationManager;
    private final PropertyTypeRepository propertyTypeRepository;
    private final PropertyTypeParser propertyTypeParser;
    private final ApiUtils apiUtils;

    @Inject
    public PropertyTypeService(final AuthorizationManager authorizationManager,
                               final PropertyTypeRepository propertyTypeRepository,
                               final PropertyTypeParser propertyTypeParser,
                               final ApiUtils apiUtils) {
        this.authorizationManager = authorizationManager;
        this.propertyTypeRepository = propertyTypeRepository;
        this.propertyTypeParser = propertyTypeParser;
        this.apiUtils = apiUtils;
    }

    @Transactional
    public Set<PropertyTypeDTO> findAll() {
        return mapPropertyTypeDtos(propertyTypeRepository.findAll());
    }

    @Transactional
    public PropertyTypeDTO findByLocalName(final String propertyTypeLocalName) {
        return mapPropertyTypeDto(propertyTypeRepository.findByLocalName(propertyTypeLocalName));
    }

    @Transactional
    public Set<PropertyTypeDTO> parseAndPersistPropertyTypesFromSourceData(final String format,
                                                                           final InputStream inputStream,
                                                                           final String jsonPayload) {
        Set<PropertyType> propertyTypes;
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON: {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final Set<PropertyTypeDTO> propertyTypeDtos = propertyTypeParser.parsePropertyTypesFromJson(jsonPayload);
                    propertyTypes = updatePropertyTypeEntities(propertyTypeDtos);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            }
            case FORMAT_EXCEL: {
                final Set<PropertyTypeDTO> propertyTypeDtos = propertyTypeParser.parsePropertyTypesFromExcelInputStream(inputStream);
                propertyTypes = updatePropertyTypeEntities(propertyTypeDtos);
                break;
            }
            case FORMAT_CSV: {
                final Set<PropertyTypeDTO> propertyTypeDtos = propertyTypeParser.parsePropertyTypesFromCsvInputStream(inputStream);
                propertyTypes = updatePropertyTypeEntities(propertyTypeDtos);
                break;
            }
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return mapPropertyTypeDtos(propertyTypes);
    }

    public PropertyType updatePropertyTypeEntity(final PropertyTypeDTO propertyTypeDTO) {
        PropertyType propertyType = createOrUpdatePropertyType(propertyTypeDTO);
        propertyTypeRepository.save(propertyType);
        return propertyType;
    }

    public Set<PropertyType> updatePropertyTypeEntities(final Set<PropertyTypeDTO> propertyTypeDtos) {
        final Set<PropertyType> propertyTypes = new HashSet<>();
        for (final PropertyTypeDTO propertyTypeDto : propertyTypeDtos) {
            final PropertyType propertyType = createOrUpdatePropertyType(propertyTypeDto);
            if (propertyType != null) {
                propertyTypes.add(propertyType);
            }
        }
        if (!propertyTypes.isEmpty()) {
            propertyTypeRepository.save(propertyTypes);
        }
        return propertyTypes;
    }

    @Transactional
    public PropertyTypeDTO parseAndPersistPropertyTypeFromJson(final String propertyTypeId,
                                                               final String jsonPayload) {
        final PropertyType existingPropertyType = propertyTypeRepository.findById(UUID.fromString(propertyTypeId));
        final PropertyType propertyType;
        if (existingPropertyType != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final PropertyTypeDTO propertyTypeDto = propertyTypeParser.parsePropertyTypeFromJson(jsonPayload);
                    if (!propertyTypeId.equalsIgnoreCase(propertyTypeDto.getId().toString())) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_ID_MISMATCH));
                    }
                    propertyType = updatePropertyTypeEntity(propertyTypeDto);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistPropertyTypeFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapPropertyTypeDto(propertyType);
    }

    private PropertyType createOrUpdatePropertyType(final PropertyTypeDTO fromPropertyType) {
        PropertyType existingPropertyType = null;
        if (fromPropertyType.getId() != null) {
            existingPropertyType = propertyTypeRepository.findById(fromPropertyType.getId());
        } else {
            existingPropertyType = null;
        }
        final PropertyType propertyType;
        if (existingPropertyType != null) {
            propertyType = updatePropertyType(existingPropertyType, fromPropertyType);
        } else {
            propertyType = createPropertyType(fromPropertyType);
        }
        return propertyType;
    }

    private PropertyType updatePropertyType(final PropertyType existingPropertyType,
                                            final PropertyTypeDTO fromPropertyType) {
        final String uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, fromPropertyType.getId().toString());
        if (!Objects.equals(existingPropertyType.getPropertyUri(), fromPropertyType.getPropertyUri())) {
            existingPropertyType.setPropertyUri(fromPropertyType.getPropertyUri());
        }
        if (!Objects.equals(existingPropertyType.getUri(), uri)) {
            existingPropertyType.setUri(uri);
        }
        if (!Objects.equals(existingPropertyType.getContext(), fromPropertyType.getContext())) {
            existingPropertyType.setUri(fromPropertyType.getContext());
        }
        if (!Objects.equals(existingPropertyType.getLocalName(), fromPropertyType.getLocalName())) {
            existingPropertyType.setLocalName(fromPropertyType.getLocalName());
        }
        if (!Objects.equals(existingPropertyType.getType(), fromPropertyType.getType())) {
            existingPropertyType.setType(fromPropertyType.getType());
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingPropertyType.getPrefLabel(language), value)) {
                existingPropertyType.setPrefLabel(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingPropertyType.getDefinition(language), value)) {
                existingPropertyType.setDefinition(language, value);
            }
        }
        return existingPropertyType;
    }

    private PropertyType createPropertyType(final PropertyTypeDTO fromPropertyType) {
        final PropertyType propertyType = new PropertyType();
        final String uri;
        if (fromPropertyType.getId() != null) {
            propertyType.setId(fromPropertyType.getId());
            uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, fromPropertyType.getId().toString());
        } else {
            final UUID uuid = UUID.randomUUID();
            uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, uuid.toString());
            propertyType.setId(uuid);
        }
        propertyType.setContext(fromPropertyType.getContext());
        propertyType.setLocalName(fromPropertyType.getLocalName());
        propertyType.setType(fromPropertyType.getType());
        propertyType.setUri(uri);
        propertyType.setPropertyUri(fromPropertyType.getPropertyUri());
        for (final Map.Entry<String, String> entry : fromPropertyType.getPrefLabel().entrySet()) {
            propertyType.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getDefinition().entrySet()) {
            propertyType.setDefinition(entry.getKey(), entry.getValue());
        }
        return propertyType;
    }
}
