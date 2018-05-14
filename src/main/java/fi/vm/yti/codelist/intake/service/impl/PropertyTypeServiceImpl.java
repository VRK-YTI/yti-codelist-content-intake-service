package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.PropertyTypeDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.parser.impl.PropertyTypeParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class PropertyTypeServiceImpl extends BaseService implements PropertyTypeService {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyTypeServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final PropertyTypeDao propertyTypeDao;
    private final PropertyTypeParserImpl propertyTypeParser;

    @Inject
    public PropertyTypeServiceImpl(final AuthorizationManager authorizationManager,
                                   final PropertyTypeDao propertyTypeRepository,
                                   final PropertyTypeParserImpl propertyTypeParser,
                                   final ApiUtils apiUtils,
                                   final DataSource dataSource) {
        super(apiUtils, dataSource);
        this.authorizationManager = authorizationManager;
        this.propertyTypeDao = propertyTypeRepository;
        this.propertyTypeParser = propertyTypeParser;
    }

    @Transactional
    public Set<PropertyTypeDTO> findAll() {
        return mapPropertyTypeDtos(propertyTypeDao.findAll());
    }

    @Transactional
    public PropertyTypeDTO findByLocalName(final String propertyTypeLocalName) {
        return mapPropertyTypeDto(propertyTypeDao.findByLocalName(propertyTypeLocalName));
    }

    @Transactional
    public Set<PropertyTypeDTO> parseAndPersistPropertyTypesFromSourceData(final String format,
                                                                           final InputStream inputStream,
                                                                           final String jsonPayload) {
        return parseAndPersistPropertyTypesFromSourceData(false, format, inputStream, jsonPayload);
    }

    @Transactional
    public Set<PropertyTypeDTO> parseAndPersistPropertyTypesFromSourceData(final boolean internal,
                                                                           final String format,
                                                                           final InputStream inputStream,
                                                                           final String jsonPayload) {
        Set<PropertyType> propertyTypes;
        if (!internal && !authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    propertyTypes = propertyTypeDao.updatePropertyTypesFromDtos(propertyTypeParser.parsePropertyTypesFromJson(jsonPayload));
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                propertyTypes = propertyTypeDao.updatePropertyTypesFromDtos(propertyTypeParser.parsePropertyTypesFromExcelInputStream(inputStream));
                break;
            case FORMAT_CSV:
                propertyTypes = propertyTypeDao.updatePropertyTypesFromDtos(propertyTypeParser.parsePropertyTypesFromCsvInputStream(inputStream));
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return mapPropertyTypeDtos(propertyTypes);
    }

    @Transactional
    public PropertyTypeDTO parseAndPersistPropertyTypeFromJson(final String propertyTypeId,
                                                               final String jsonPayload) {
        final PropertyType existingPropertyType = propertyTypeDao.findById(UUID.fromString(propertyTypeId));
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
                    propertyType = propertyTypeDao.updatePropertyTypeFromDto(propertyTypeDto);
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
