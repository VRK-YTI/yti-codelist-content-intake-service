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

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.parser.ExternalReferenceParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class ExternalReferenceServiceImpl extends BaseService implements ExternalReferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalReferenceServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final ExternalReferenceParser externalReferenceParser;
    private final ExternalReferenceDao externalReferenceDao;

    @Inject
    public ExternalReferenceServiceImpl(final AuthorizationManager authorizationManager,
                                        final ExternalReferenceRepository externalReferenceRepository,
                                        final ExternalReferenceParser externalReferenceParser,
                                        final ExternalReferenceDao externalReferenceDao) {
        this.authorizationManager = authorizationManager;
        this.externalReferenceRepository = externalReferenceRepository;
        this.externalReferenceParser = externalReferenceParser;
        this.externalReferenceDao = externalReferenceDao;
    }

    @Transactional
    public Set<ExternalReferenceDTO> findAll() {
        return mapDeepExternalReferenceDtos(externalReferenceRepository.findAll());
    }

    @Transactional
    public Set<ExternalReferenceDTO> findByCodeSchemeId(final UUID codeSchemeId) {
        return mapDeepExternalReferenceDtos(externalReferenceRepository.findByParentCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromSourceData(final String format,
                                                                                     final InputStream inputStream,
                                                                                     final String jsonPayload,
                                                                                     final CodeScheme codeScheme) {
        Set<ExternalReference> externalReferences;
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(externalReferenceParser.parseExternalReferencesFromJson(jsonPayload), codeScheme);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(externalReferenceParser.parseExternalReferencesFromExcelInputStream(inputStream), codeScheme);
                break;
            case FORMAT_CSV:
                externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(externalReferenceParser.parseExternalReferencesFromCsvInputStream(inputStream), codeScheme);
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return mapDeepExternalReferenceDtos(externalReferences);
    }

    @Transactional
    public ExternalReferenceDTO parseAndPersistExternalReferenceFromJson(final String externalReferenceId,
                                                                         final String jsonPayload,
                                                                         final CodeScheme codeScheme) {
        final ExternalReference existingExternalReference = externalReferenceRepository.findById(UUID.fromString(externalReferenceId));
        final ExternalReference externalReference;
        if (existingExternalReference != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
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
        return mapDeepExternalReferenceDto(externalReference);
    }
}