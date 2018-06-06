package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.parser.ExtensionParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class ExtensionServiceImpl extends BaseService implements ExtensionService {

    private final AuthorizationManager authorizationManager;
    private final ExtensionDao extensionDao;
    private final ExtensionParser extensionParser;
    private final ExtensionSchemeDao extensionSchemeDao;
    private final CodeSchemeDao codeSchemeDao;

    @Inject
    public ExtensionServiceImpl(final AuthorizationManager authorizationManager,
                                final ExtensionDao extensionDao,
                                final ExtensionParser extensionParser,
                                final ExtensionSchemeDao extensionSchemeDao,
                                final CodeSchemeDao codeSchemeDao,
                                final ApiUtils apiUtils,
                                final CommitRepository commitRepository) {
        super(apiUtils, commitRepository);
        this.authorizationManager = authorizationManager;
        this.extensionDao = extensionDao;
        this.extensionParser = extensionParser;
        this.extensionSchemeDao = extensionSchemeDao;
        this.codeSchemeDao = codeSchemeDao;
    }

    @Transactional
    public ExtensionDTO deleteExtension(final UUID id) {
        final Extension extension = extensionDao.findById(id);
        if (!authorizationManager.canExtensionBeDeleted(extension)) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final ExtensionDTO extensionDto = mapExtensionDto(extension, false);
        extensionDao.delete(extension);
        return extensionDto;
    }

    @Transactional
    public Set<ExtensionDTO> findAll() {
        return mapDeepExtensionDtos(extensionDao.findAll());
    }

    @Transactional
    public ExtensionDTO findById(final UUID id) {
        return mapDeepExtensionDto(extensionDao.findById(id));
    }

    @Transactional
    public Set<ExtensionDTO> findByExtensionSchemeId(final UUID id) {
        return mapDeepExtensionDtos(extensionDao.findByExtensionSchemeId(id));
    }

    @Transactional
    public ExtensionDTO parseAndPersistExtensionFromJson(final String jsonPayload) {
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Extension extension = null;
        if (jsonPayload != null && !jsonPayload.isEmpty()) {
            final ExtensionDTO extensionDto = extensionParser.parseExtensionFromJson(jsonPayload);
            if (extensionDto.getExtensionScheme() != null) {
                final ExtensionScheme extensionScheme = extensionSchemeDao.findById(extensionDto.getExtensionScheme().getId());
                extension = extensionDao.updateExtensionEntityFromDto(extensionScheme, extensionDto);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (extension == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapDeepExtensionDto(extension);
    }

    @Transactional
    public Set<ExtensionDTO> parseAndPersistExtensionsFromSourceData(final String codeRegistryCodeValue,
                                                                     final String codeSchemeCodeValue,
                                                                     final String extensionSchemeCodeValue,
                                                                     final String format,
                                                                     final InputStream inputStream,
                                                                     final String jsonPayload,
                                                                     final String sheetName) {
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Set<Extension> extensions;
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final ExtensionScheme extensionScheme = extensionSchemeDao.findByParentCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionSchemeCodeValue);
            if (extensionScheme != null) {
                switch (format.toLowerCase()) {
                    case FORMAT_JSON:
                        if (jsonPayload != null && !jsonPayload.isEmpty()) {
                            extensions = extensionDao.updateExtensionEntitiesFromDtos(extensionScheme, extensionParser.parseExtensionsFromJson(jsonPayload));
                        } else {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                        }
                        break;
                    case FORMAT_EXCEL:
                        extensions = extensionDao.updateExtensionEntitiesFromDtos(extensionScheme, extensionParser.parseExtensionsFromExcelInputStream(inputStream, sheetName));
                        break;
                    case FORMAT_CSV:
                        extensions = extensionDao.updateExtensionEntitiesFromDtos(extensionScheme, extensionParser.parseExtensionsFromCsvInputStream(inputStream));
                        break;
                    default:
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
                return mapDeepExtensionDtos(extensions);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    public Set<ExtensionDTO> parseAndPersistExtensionsFromExcelWorkbook(final ExtensionScheme extensionScheme,
                                                                        final Workbook workbook,
                                                                        final String sheetName) {
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Set<Extension> extensions;
        final Set<ExtensionDTO> extensionDtos = extensionParser.parseExtensionsFromExcelWorkbook(workbook, sheetName);
        extensions = extensionDao.updateExtensionEntitiesFromDtos(extensionScheme, extensionDtos);
        return mapDeepExtensionDtos(extensions);
    }
}
