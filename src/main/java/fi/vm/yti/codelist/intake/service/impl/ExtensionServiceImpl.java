package fi.vm.yti.codelist.intake.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Singleton;

import org.apache.poi.EmptyFileException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.parser.ExtensionParser;
import fi.vm.yti.codelist.intake.parser.MemberParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class ExtensionServiceImpl implements ExtensionService {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionServiceImpl.class);

    private final ExtensionDao extensionDao;
    private final MemberDao memberDao;
    private final CodeSchemeDao codeSchemeDao;
    private final ExtensionParser extensionParser;
    private final MemberParser memberParser;
    private final AuthorizationManager authorizationManager;
    private final DtoMapperService dtoMapperService;

    public ExtensionServiceImpl(final ExtensionDao extensionDao,
                                final MemberDao memberDao,
                                final CodeSchemeDao codeSchemeDao,
                                final ExtensionParser extensionParser,
                                final AuthorizationManager authorizationManager,
                                final MemberParser memberParser,
                                final DtoMapperService dtoMapperService) {
        this.extensionDao = extensionDao;
        this.memberDao = memberDao;
        this.codeSchemeDao = codeSchemeDao;
        this.extensionParser = extensionParser;
        this.authorizationManager = authorizationManager;
        this.memberParser = memberParser;
        this.dtoMapperService = dtoMapperService;
    }

    @Transactional
    public int getExtensionCount() {
        return extensionDao.getExtensionCount();
    }

    @Transactional
    public Set<ExtensionDTO> findAll() {
        return dtoMapperService.mapDeepExtensionDtos(extensionDao.findAll());
    }

    @Transactional
    public Set<ExtensionDTO> findAll(final PageRequest pageRequest) {
        final Set<Extension> extensions = extensionDao.findAll(pageRequest);
        return dtoMapperService.mapDeepExtensionDtos(extensions);
    }

    @Transactional
    public ExtensionDTO findById(final UUID id) {
        final Extension extension = extensionDao.findById(id);
        if (extension == null) {
            return null;
        }
        return dtoMapperService.mapDeepExtensionDto(extensionDao.findById(id));
    }

    @Transactional
    public Set<ExtensionDTO> findByCodeSchemeId(final UUID codeSchemeId) {
        final CodeScheme codeScheme = codeSchemeDao.findById(codeSchemeId);
        return dtoMapperService.mapDeepExtensionDtos(extensionDao.findByCodeSchemes(codeScheme));
    }

    @Transactional
    public Set<ExtensionDTO> findByParentCodeSchemeId(final UUID codeSchemeId) {
        return dtoMapperService.mapDeepExtensionDtos(extensionDao.findByParentCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public ExtensionDTO findByCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                       final String codeValue) {
        final Extension extension = extensionDao.findByParentCodeSchemeIdAndCodeValue(codeSchemeId, codeValue);
        if (extension == null) {
            return null;
        }
        return dtoMapperService.mapDeepExtensionDto(extension);
    }

    @Transactional
    public Set<ExtensionDTO> parseAndPersistExtensionsFromSourceData(final String codeRegistryCodeValue,
                                                                     final String codeSchemeCodeValue,
                                                                     final String format,
                                                                     final InputStream inputStream,
                                                                     final String jsonPayload,
                                                                     final String sheetName,
                                                                     final boolean autoCreateMembers) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            Set<Extension> extensions;
            switch (format.toLowerCase()) {
                case FORMAT_JSON:
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        extensions = extensionDao.updateExtensionEntitiesFromDtos(codeScheme, extensionParser.parseExtensionsFromJson(jsonPayload), autoCreateMembers);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                    }
                    break;
                case FORMAT_EXCEL:
                    try {
                        final Map<ExtensionDTO, String> membersSheetNames = new HashMap<>();
                        final Workbook workbook = WorkbookFactory.create(inputStream);
                        extensions = extensionDao.updateExtensionEntitiesFromDtos(codeScheme, extensionParser.parseExtensionsFromExcelWorkbook(workbook, sheetName, membersSheetNames), autoCreateMembers);
                        if (!membersSheetNames.isEmpty()) {
                            membersSheetNames.forEach((extensionDto, membersSheetName) -> extensions.forEach(extension -> {
                                if (extension.getCodeValue().equalsIgnoreCase(extensionDto.getCodeValue())) {
                                    final Map<String, LinkedHashSet<MemberDTO>> memberDTOsToBeDeletedPerExtension = new HashMap<>();
                                    memberDao.updateMemberEntitiesFromDtos(extension, memberParser.parseMembersFromExcelWorkbook(extension, workbook, membersSheetName, memberDTOsToBeDeletedPerExtension, codeScheme.getStatus()));
                                }
                            }));
                        }
                    } catch (final EmptyFileException | IOException e) {
                        LOG.error("Error parsing Excel file!", e);
                        throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
                    }
                    break;
                case FORMAT_CSV:
                    extensions = extensionDao.updateExtensionEntitiesFromDtos(codeScheme, extensionParser.parseExtensionsFromCsvInputStream(inputStream), autoCreateMembers);
                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_INVALID_FORMAT));
            }
            return dtoMapperService.mapDeepExtensionDtos(extensions);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
        }
    }

    @Transactional
    public Set<ExtensionDTO> parseAndPersistExtensionsFromExcelWorkbook(final CodeScheme codeScheme,
                                                                        final Workbook workbook,
                                                                        final String sheetName,
                                                                        final Map<ExtensionDTO, String> membersSheetNames,
                                                                        final boolean autoCreateMembers) {
        if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        final Set<ExtensionDTO> extensionDtos = extensionParser.parseExtensionsFromExcelWorkbook(workbook, sheetName, membersSheetNames);
        final Set<Extension> extensions = extensionDao.updateExtensionEntitiesFromDtos(codeScheme, extensionDtos, autoCreateMembers);
        extensionDtos.forEach(extensionDto -> extensions.forEach(extension -> {
            if (extension.getCodeValue().equalsIgnoreCase(extensionDto.getCodeValue())) {
                extensionDto.setId(extension.getId());
            }
        }));
        return dtoMapperService.mapDeepExtensionDtos(extensions);
    }

    @Transactional
    public ExtensionDTO parseAndPersistExtensionFromJson(final String codeRegistryCodeValue,
                                                         final String codeSchemeCodeValue,
                                                         final String extensionCodeValue,
                                                         final String jsonPayload,
                                                         final boolean autoCreateMembers) {
        final CodeScheme parentCodeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (parentCodeScheme != null) {
            final Extension existingExtension = extensionDao.findByParentCodeSchemeIdAndCodeValue(parentCodeScheme.getId(), extensionCodeValue);
            final Extension extension;
            if (existingExtension != null) {
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ExtensionDTO extensionDTO = extensionParser.parseExtensionFromJson(jsonPayload);
                        if (!authorizationManager.canBeModifiedByUserInOrganization(parentCodeScheme.getOrganizations())) {
                            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
                        }
                        extension = extensionDao.updateExtensionEntityFromDto(parentCodeScheme, extensionDTO, autoCreateMembers);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                    }
                } catch (final YtiCodeListException e) {
                    throw e;
                } catch (final Exception e) {
                    LOG.error("Caught exception in parseAndPersistExtensionFromJson.", e);
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PARSING_ERROR));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_NOT_FOUND));
            }
            return dtoMapperService.mapDeepExtensionDto(extension);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
        }
    }

    @Transactional
    public ExtensionDTO parseAndPersistExtensionFromJson(final UUID extensionId,
                                                         final String jsonPayload,
                                                         final boolean autoCreateMembers) {
        final Extension existingExtension = extensionDao.findById(extensionId);
        final Extension extension;
        if (existingExtension != null) {
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final ExtensionDTO extensionDto = extensionParser.parseExtensionFromJson(jsonPayload);
                    if (extensionDto.getId() != extensionId) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    final CodeScheme codeScheme = codeSchemeDao.findById(extensionDto.getParentCodeScheme().getId());
                    if (codeScheme == null) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
                    }
                    if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
                        throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
                    }
                    extension = extensionDao.updateExtensionEntityFromDto(codeScheme, extensionDto, autoCreateMembers);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistExtensionFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PARSING_ERROR));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_NOT_FOUND));
        }
        return dtoMapperService.mapDeepExtensionDto(extension);
    }

    @Transactional
    public ExtensionDTO deleteExtension(final UUID extensionId) {
        final Extension extension = extensionDao.findById(extensionId);
        if (authorizationManager.canExtensionBeDeleted(extension)) {
            final ExtensionDTO extensionDto = dtoMapperService.mapExtensionDto(extension, false);
            extensionDao.delete(extension);
            return extensionDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }
}
