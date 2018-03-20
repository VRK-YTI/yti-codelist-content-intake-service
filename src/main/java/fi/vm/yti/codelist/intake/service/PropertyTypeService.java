package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
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

    @Inject
    public PropertyTypeService(final AuthorizationManager authorizationManager,
                               final PropertyTypeRepository propertyTypeRepository,
                               final PropertyTypeParser propertyTypeParser) {
        this.authorizationManager = authorizationManager;
        this.propertyTypeRepository = propertyTypeRepository;
        this.propertyTypeParser = propertyTypeParser;
    }

    @Transactional
    public Set<PropertyTypeDTO> findAll() {
        return mapPropertyTypeDtos(propertyTypeRepository.findAll());
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
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    propertyTypes = propertyTypeParser.parsePropertyTypesFromJson(jsonPayload);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                propertyTypes = propertyTypeParser.parsePropertyTypesFromExcelInputStream(inputStream);
                break;
            case FORMAT_CSV:
                propertyTypes = propertyTypeParser.parsePropertyTypesFromCsvInputStream(inputStream);
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        if (propertyTypes != null && !propertyTypes.isEmpty()) {
            propertyTypeRepository.save(propertyTypes);
        }
        return mapPropertyTypeDtos(propertyTypes);
    }

    @Transactional
    public PropertyTypeDTO parseAndPersistPropertyTypeFromJson(final String PropertyTypeId,
                                                               final String jsonPayload) {
        final PropertyType existingPropertyType = propertyTypeRepository.findById(UUID.fromString(PropertyTypeId));
        final PropertyType propertyType;
        if (existingPropertyType != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    propertyType = propertyTypeParser.parsePropertyTypeFromJson(jsonPayload);
                    if (!existingPropertyType.getId().toString().equalsIgnoreCase(PropertyTypeId)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_ID_MISMATCH));
                    }
                    propertyTypeRepository.save(propertyType);
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
}
