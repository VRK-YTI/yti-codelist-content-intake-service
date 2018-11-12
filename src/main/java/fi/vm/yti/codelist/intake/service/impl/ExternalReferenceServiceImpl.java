package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.parser.ExternalReferenceParser;
import fi.vm.yti.codelist.intake.parser.impl.ExternalReferenceParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class ExternalReferenceServiceImpl implements ExternalReferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalReferenceServiceImpl.class);

    private final AuthorizationManager authorizationManager;
    private final ExternalReferenceParser externalReferenceParser;
    private final ExternalReferenceDao externalReferenceDao;
    private final DtoMapperService dtoMapperService;

    @Inject
    public ExternalReferenceServiceImpl(final AuthorizationManager authorizationManager,
                                        final ExternalReferenceParserImpl externalReferenceParser,
                                        final ExternalReferenceDao externalReferenceDao,
                                        final DtoMapperService dtoMapperService) {
        this.authorizationManager = authorizationManager;
        this.externalReferenceParser = externalReferenceParser;
        this.externalReferenceDao = externalReferenceDao;
        this.dtoMapperService = dtoMapperService;
    }

    @Transactional
    public Set<ExternalReferenceDTO> findAll() {
        return dtoMapperService.mapDeepExternalReferenceDtos(externalReferenceDao.findAll());
    }

    @Transactional
    public Set<ExternalReferenceDTO> findByParentCodeSchemeId(final UUID codeSchemeId) {
        return dtoMapperService.mapDeepExternalReferenceDtos(externalReferenceDao.findByParentCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public ExternalReferenceDTO findByParentCodeSchemeIdAndHref(final UUID codeSchemeId,
                                                                final String href) {
        return dtoMapperService.mapDeepExternalReferenceDto(externalReferenceDao.findByParentCodeSchemeIdAndHref(codeSchemeId, href));
    }

    @Transactional
    public Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromExcelWorkbook(final Workbook workbook,
                                                                                        final String sheetName,
                                                                                        final CodeScheme codeScheme) {
        final Set<ExternalReference> externalReferences;
        if (codeScheme != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            final Set<ExternalReferenceDTO> externalReferenceDtos = externalReferenceParser.parseExternalReferencesFromExcelWorkbook(workbook, sheetName, codeScheme);
            externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(externalReferenceDtos, codeScheme);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return dtoMapperService.mapDeepExternalReferenceDtos(externalReferences);
    }

    @Transactional
    public Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromSourceData(final String format,
                                                                                     final InputStream inputStream,
                                                                                     final String jsonPayload,
                                                                                     final CodeScheme codeScheme) {
        return parseAndPersistExternalReferencesFromSourceData(false, format, inputStream, jsonPayload, codeScheme);
    }

    @Transactional
    public Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromSourceData(final boolean isAuthorized,
                                                                                     final String format,
                                                                                     final InputStream inputStream,
                                                                                     final String jsonPayload,
                                                                                     final CodeScheme codeScheme) {
        if (!isAuthorized && !authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Set<ExternalReference> externalReferences;
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(isAuthorized, externalReferenceParser.parseExternalReferencesFromJson(jsonPayload), codeScheme);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(isAuthorized, externalReferenceParser.parseExternalReferencesFromExcelInputStream(inputStream, ApiConstants.EXCEL_SHEET_LINKS), codeScheme);
                break;
            case FORMAT_CSV:
                externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(isAuthorized, externalReferenceParser.parseExternalReferencesFromCsvInputStream(inputStream), codeScheme);
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return dtoMapperService.mapDeepExternalReferenceDtos(externalReferences);
    }

    @Transactional
    public ExternalReferenceDTO parseAndPersistExternalReferenceFromJson(final String externalReferenceId,
                                                                         final String jsonPayload,
                                                                         final CodeScheme codeScheme) {
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final ExternalReference existingExternalReference = externalReferenceDao.findById(UUID.fromString(externalReferenceId));
        final ExternalReference externalReference;
        if (existingExternalReference != null) {
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final ExternalReferenceDTO externalReferenceDto = externalReferenceParser.parseExternalReferenceFromJson(jsonPayload);
                    if (!externalReferenceDto.getId().toString().equalsIgnoreCase(externalReferenceId)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    externalReference = externalReferenceDao.updateExternalReferenceFromDto(externalReferenceDto, codeScheme);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistExternalReferenceFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return dtoMapperService.mapDeepExternalReferenceDto(externalReference);
    }
}
