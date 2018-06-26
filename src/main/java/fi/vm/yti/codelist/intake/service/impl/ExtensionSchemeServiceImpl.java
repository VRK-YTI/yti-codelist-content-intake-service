package fi.vm.yti.codelist.intake.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.parser.ExtensionParser;
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
    private final ExtensionParser extensionParser;
    private final AuthorizationManager authorizationManager;

    public ExtensionSchemeServiceImpl(final ExtensionSchemeDao extensionSchemeDao,
                                      final ExtensionDao extensionDao,
                                      final CodeSchemeDao codeSchemeDao,
                                      final ExtensionSchemeParser extensionSchemeParser,
                                      final AuthorizationManager authorizationManager,
                                      final ApiUtils apiUtils,
                                      final ExtensionParser extensionParser) {
        super(apiUtils);
        this.extensionSchemeDao = extensionSchemeDao;
        this.extensionDao = extensionDao;
        this.codeSchemeDao = codeSchemeDao;
        this.extensionSchemeParser = extensionSchemeParser;
        this.authorizationManager = authorizationManager;
        this.extensionParser = extensionParser;
    }

    @Transactional
    public Set<ExtensionSchemeDTO> findAll() {
        return mapDeepExtensionSchemeDtos(extensionSchemeDao.findAll());
    }

    @Transactional
    public ExtensionSchemeDTO findById(final UUID id) {
        final ExtensionScheme extensionScheme = extensionSchemeDao.findById(id);
        if (extensionScheme == null) {
            return null;
        }
        return mapDeepExtensionSchemeDto(extensionSchemeDao.findById(id));
    }

    @Transactional
    public Set<ExtensionSchemeDTO> findByCodeSchemeId(final UUID codeSchemeId) {
        return mapDeepExtensionSchemeDtos(extensionSchemeDao.findByParentCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public ExtensionSchemeDTO findByCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                             final String codeValue) {
        final ExtensionScheme extensionScheme = extensionSchemeDao.findByParentCodeSchemeIdAndCodeValue(codeSchemeId, codeValue);
        if (extensionScheme == null) {
            return null;
        }
        return mapDeepExtensionSchemeDto(extensionScheme);
    }

    @Transactional
    public ExtensionSchemeDTO findByCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                                           final String codeValue) {
        final ExtensionScheme extensionScheme = extensionSchemeDao.findByParentCodeSchemeAndCodeValue(codeScheme, codeValue);
        if (extensionScheme == null) {
            return null;
        }
        return mapDeepExtensionSchemeDto(extensionScheme);
    }

    @Transactional
    public Set<ExtensionSchemeDTO> parseAndPersistExtensionSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                                 final String codeSchemeCodeValue,
                                                                                 final String format,
                                                                                 final InputStream inputStream,
                                                                                 final String jsonPayload,
                                                                                 final String sheetName) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getCodeRegistry().getOrganizations())) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        Set<ExtensionScheme> extensionSchemes;
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
                    try {
                        final Map<ExtensionSchemeDTO, String> extensionsSheetNames = new HashMap<>();
                        final Workbook workbook = WorkbookFactory.create(inputStream);
                        extensionSchemes = extensionSchemeDao.updateExtensionSchemeEntitiesFromDtos(codeScheme, extensionSchemeParser.parseExtensionSchemesFromExcelWorkbook(workbook, sheetName, extensionsSheetNames));
                        if (!extensionsSheetNames.isEmpty()) {
                            extensionsSheetNames.forEach((extensionSchemeDto, extensionsSheetName) -> extensionSchemes.forEach(extensionScheme -> {
                                if (extensionScheme.getCodeValue().equalsIgnoreCase(extensionSchemeDto.getCodeValue())) {
                                    extensionDao.updateExtensionEntitiesFromDtos(extensionScheme, extensionParser.parseExtensionsFromExcelWorkbook(workbook, extensionsSheetName));
                                }
                            }));
                        }
                    } catch (final InvalidFormatException | IOException | POIXMLException e) {
                        LOG.error("Error parsing Excel file!", e);
                        throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
                    }
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
                                                                                    final String sheetName,
                                                                                    final Map<ExtensionSchemeDTO, String> extensionsSheetNames) {
        if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getCodeRegistry().getOrganizations())) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final Set<ExtensionSchemeDTO> extensionSchemeDtos = extensionSchemeParser.parseExtensionSchemesFromExcelWorkbook(workbook, sheetName, extensionsSheetNames);
        final Set<ExtensionScheme> extensionSchemes = extensionSchemeDao.updateExtensionSchemeEntitiesFromDtos(codeScheme, extensionSchemeDtos);
        extensionSchemeDtos.forEach(extensionSchemeDto -> extensionSchemes.forEach(extensionScheme -> {
            if (extensionScheme.getCodeValue().equalsIgnoreCase(extensionSchemeDto.getCodeValue())) {
                extensionSchemeDto.setId(extensionScheme.getId());
            }
        }));
        return mapDeepExtensionSchemeDtos(extensionSchemes);
    }

    @Transactional
    public ExtensionSchemeDTO parseAndPersistExtensionSchemeFromJson(final String codeRegistryCodeValue,
                                                                     final String codeSchemeCodeValue,
                                                                     final String extensionSchemeCodeValue,
                                                                     final String jsonPayload) {
        final CodeScheme parentCodeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (parentCodeScheme != null) {
            final ExtensionScheme existingExtensionScheme = extensionSchemeDao.findByParentCodeSchemeIdAndCodeValue(parentCodeScheme.getId(), extensionSchemeCodeValue);
            final ExtensionScheme extensionScheme;
            if (existingExtensionScheme != null) {
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ExtensionSchemeDTO extensionSchemeDTO = extensionSchemeParser.parseExtensionSchemeFromJson(jsonPayload);
                        if (!authorizationManager.canBeModifiedByUserInOrganization(parentCodeScheme.getCodeRegistry().getOrganizations())) {
                            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
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
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    @Transactional
    public ExtensionSchemeDTO parseAndPersistExtensionSchemeFromJson(final UUID extensionSchemeId,
                                                                     final String jsonPayload) {
        final ExtensionScheme existingExtensionScheme = extensionSchemeDao.findById(extensionSchemeId);
        final ExtensionScheme extensionScheme;
        if (existingExtensionScheme != null) {
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final ExtensionSchemeDTO extensionSchemeDTO = extensionSchemeParser.parseExtensionSchemeFromJson(jsonPayload);
                    if (extensionSchemeDTO.getId() != extensionSchemeId) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    final CodeScheme codeScheme = codeSchemeDao.findById(extensionSchemeDTO.getId());
                    if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getCodeRegistry().getOrganizations())) {
                        throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
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

    @Transactional
    public ExtensionSchemeDTO deleteExtensionScheme(final UUID extensionSchemeId) {
        final ExtensionScheme extensionScheme = extensionSchemeDao.findById(extensionSchemeId);
        if (authorizationManager.canExtensionSchemeBeDeleted(extensionScheme)) {
            final ExtensionSchemeDTO extensionSchemeDto = mapExtensionSchemeDto(extensionScheme, false);
            extensionSchemeDao.delete(extensionScheme);
            return extensionSchemeDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }
}
