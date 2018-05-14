package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;
import javax.transaction.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.parser.ExtensionSchemeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.ExtensionSchemeService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class ExtensionSchemeServiceImpl extends BaseService implements ExtensionSchemeService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionSchemeServiceImpl.class);

    private final ExtensionSchemeDao extensionSchemeDao;
    private final ExtensionDao extensionDao;
    private final CodeSchemeDao codeSchemeDao;
    private final ExtensionSchemeParser extensionSchemeParser;
    private final AuthorizationManager authorizationManager;

    public ExtensionSchemeServiceImpl(final ExtensionSchemeDao extensionSchemeDao,
                                      final ExtensionDao extensionDao,
                                      final CodeSchemeDao codeSchemeDao,
                                      final ExtensionSchemeParser extensionSchemeParser,
                                      final AuthorizationManager authorizationManager,
                                      final ApiUtils apiUtils,
                                      final DataSource dataSource) {
        super(apiUtils, dataSource);
        this.extensionSchemeDao = extensionSchemeDao;
        this.extensionDao = extensionDao;
        this.codeSchemeDao = codeSchemeDao;
        this.extensionSchemeParser = extensionSchemeParser;
        this.authorizationManager = authorizationManager;
    }

    @Transactional
    public Set<ExtensionSchemeDTO> findAll() {
        return mapDeepExtensionSchemeDtos(extensionSchemeDao.findAll());
    }

    @Transactional
    public ExtensionSchemeDTO findById(final UUID id) {
        return mapDeepExtensionSchemeDto(extensionSchemeDao.findById(id));
    }

    @Transactional
    public Set<ExtensionSchemeDTO> findByCodeSchemeId(final UUID codeSchemeId) {
        return mapDeepExtensionSchemeDtos(extensionSchemeDao.findByCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public ExtensionSchemeDTO findByCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                             final String codeValue) {
        return mapDeepExtensionSchemeDto(extensionSchemeDao.findByCodeSchemeIdAndCodeValue(codeSchemeId, codeValue));
    }

    @Transactional
    public ExtensionSchemeDTO findByCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                                           final String codeValue) {
        return mapDeepExtensionSchemeDto(extensionSchemeDao.findByCodeSchemeAndCodeValue(codeScheme, codeValue));
    }

    @Transactional
    public Set<ExtensionSchemeDTO> parseAndPersistExtensionSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                                 final String codeSchemeCodeValue,
                                                                                 final String format,
                                                                                 final InputStream inputStream,
                                                                                 final String jsonPayload) {
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Set<ExtensionScheme> extensionSchemes;
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            switch (format.toLowerCase()) {
                case FORMAT_JSON:
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        extensionSchemes = extensionSchemeDao.updateExtensionSchemeEntitiesFromDtos(codeScheme, extensionSchemeParser.parseExtensionSchemesFromJson(jsonPayload));
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                    }
                    break;
                case FORMAT_EXCEL:
                    extensionSchemes = extensionSchemeDao.updateExtensionSchemeEntitiesFromDtos(codeScheme, extensionSchemeParser.parseExtensionSchemesFromExcelInputStream(inputStream, EXCEL_SHEET_EXTENSIONSCHEMES));
                    break;
                case FORMAT_CSV:
                    extensionSchemes = extensionSchemeDao.updateExtensionSchemeEntitiesFromDtos(codeScheme, extensionSchemeParser.parseExtensionSchemesFromCsvInputStream(inputStream));
                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
            return mapDeepExtensionSchemeDtos(extensionSchemes);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    @Transactional
    public Set<ExtensionSchemeDTO> parseAndPersistExtensionSchemesFromExcelWorkbook(final CodeScheme codeScheme,
                                                                                    final Workbook workbook,
                                                                                    final String sheetName) {
        if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getCodeRegistry().getOrganizations())) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Set<ExtensionScheme> extensionSchemes;
        final Set<ExtensionSchemeDTO> extensionSchemeDtos = extensionSchemeParser.parseExtensionSchemesFromExcelWorkbook(workbook, sheetName);
        extensionSchemes = extensionSchemeDao.updateExtensionSchemeEntitiesFromDtos(codeScheme, extensionSchemeDtos);
        return mapDeepExtensionSchemeDtos(extensionSchemes);
    }

    public ExtensionSchemeDTO parseAndPersistExtensionSchemeFromJson(final UUID extensionSchemeId,
                                                                     final String jsonPayload) {
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final ExtensionScheme existingExtensionScheme = extensionSchemeDao.findById(extensionSchemeId);
        final ExtensionScheme extensionScheme;
        if (existingExtensionScheme != null) {
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final ExtensionSchemeDTO extensionSchemeDTO = extensionSchemeParser.parseExtensionSchemeFromJson(jsonPayload);
                    if (extensionSchemeDTO.getId() != extensionSchemeId) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    extensionScheme = extensionSchemeDao.updateExtensionSchemeEntityFromDtos(null, extensionSchemeDTO);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistExtensionSchemeFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapDeepExtensionSchemeDto(extensionScheme);
    }

    public ExtensionSchemeDTO deleteExtensionScheme(final UUID extensionSchemeId) {
        if (authorizationManager.isSuperUser()) {
            final ExtensionScheme extensionScheme = extensionSchemeDao.findById(extensionSchemeId);
            final ExtensionSchemeDTO extensionSchemeDto = mapExtensionSchemeDto(extensionScheme, false);
            final Set<Extension> extensions = extensionDao.findByExtensionSchemeId(extensionScheme.getId());
            if (extensions != null && !extensions.isEmpty()) {
                extensionDao.delete(extensions);
            }
            extensionSchemeDao.delete(extensionScheme);
            return extensionSchemeDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }
}
