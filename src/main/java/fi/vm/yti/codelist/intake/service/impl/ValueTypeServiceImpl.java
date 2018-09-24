package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.dao.ValueTypeDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.ValueType;
import fi.vm.yti.codelist.intake.parser.impl.ValueTypeParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.ValueTypeService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class ValueTypeServiceImpl implements ValueTypeService {

    private static final Logger LOG = LoggerFactory.getLogger(ValueTypeServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final ValueTypeDao valueTypeDao;
    private final ValueTypeParserImpl valueTypeParser;
    private final DtoMapperService dtoMapperService;

    @Inject
    public ValueTypeServiceImpl(final AuthorizationManager authorizationManager,
                                final ValueTypeDao valueTypeRepository,
                                final ValueTypeParserImpl valueTypeParser,
                                final DtoMapperService dtoMapperService) {
        this.authorizationManager = authorizationManager;
        this.valueTypeDao = valueTypeRepository;
        this.valueTypeParser = valueTypeParser;
        this.dtoMapperService = dtoMapperService;
    }

    @Transactional
    public Set<ValueTypeDTO> findAll() {
        return dtoMapperService.mapValueTypeDtos(valueTypeDao.findAll());
    }

    @Transactional
    public ValueTypeDTO findByLocalName(final String valueTypeLocalName) {
        return dtoMapperService.mapValueTypeDto(valueTypeDao.findByLocalName(valueTypeLocalName));
    }

    @Transactional
    public Set<ValueTypeDTO> parseAndPersistValueTypesFromSourceData(final String format,
                                                                     final InputStream inputStream,
                                                                     final String jsonPayload) {
        return parseAndPersistValueTypesFromSourceData(false, format, inputStream, jsonPayload);
    }

    @Transactional
    public Set<ValueTypeDTO> parseAndPersistValueTypesFromSourceData(final boolean isAuthorized,
                                                                     final String format,
                                                                     final InputStream inputStream,
                                                                     final String jsonPayload) {
        Set<ValueType> valueTypes;
        if (!isAuthorized && !authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    valueTypes = valueTypeDao.updateValueTypesFromDtos(valueTypeParser.parseValueTypesFromJson(jsonPayload));
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                valueTypes = valueTypeDao.updateValueTypesFromDtos(valueTypeParser.parseValueTypesFromExcelInputStream(inputStream));
                break;
            case FORMAT_CSV:
                valueTypes = valueTypeDao.updateValueTypesFromDtos(valueTypeParser.parseValueTypesFromCsvInputStream(inputStream));
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return dtoMapperService.mapValueTypeDtos(valueTypes);
    }

    @Transactional
    public ValueTypeDTO parseAndPersistValueTypeFromJson(final UUID valueTypeId,
                                                         final String jsonPayload) {
        final ValueType existingValueType = valueTypeDao.findById(valueTypeId);
        final ValueType valueType;
        if (existingValueType != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final ValueTypeDTO valueTypeDto = valueTypeParser.parseValueTypeFromJson(jsonPayload);
                    if (!valueTypeId.equals(valueTypeDto.getId())) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_ID_MISMATCH));
                    }
                    valueType = valueTypeDao.updateValueTypeFromDto(valueTypeDto);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistValueTypeFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return dtoMapperService.mapValueTypeDto(valueType);
    }
}
