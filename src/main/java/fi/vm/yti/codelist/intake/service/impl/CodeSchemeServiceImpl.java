package fi.vm.yti.codelist.intake.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.InconsistencyInVersionHierarchyException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.parser.impl.CodeSchemeParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExtensionSchemeService;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class CodeSchemeServiceImpl extends BaseService implements CodeSchemeService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeSchemeParserImpl codeSchemeParser;
    private final CodeService codeService;
    private final ExtensionSchemeService extensionSchemeService;
    private final ExtensionService extensionService;
    private final CodeDao codeDao;
    private final ExtensionSchemeDao extensionSchemeDao;
    private final ExternalReferenceDao externalReferenceDao;

    @Inject
    public CodeSchemeServiceImpl(final AuthorizationManager authorizationManager,
                                 final CodeRegistryDao codeRegistryDao,
                                 final CodeSchemeDao codeSchemeDao,
                                 final CodeSchemeParserImpl codeSchemeParser,
                                 final CodeService codeService,
                                 final ExtensionSchemeService extensionSchemeService,
                                 final ExtensionService extensionService,
                                 final CodeDao codeDao,
                                 final ApiUtils apiUtils,
                                 final ExtensionSchemeDao extensionSchemeDao,
                                 final ExternalReferenceDao externalReferenceDao) {
        super(apiUtils);
        this.codeRegistryDao = codeRegistryDao;
        this.authorizationManager = authorizationManager;
        this.codeSchemeParser = codeSchemeParser;
        this.codeService = codeService;
        this.codeSchemeDao = codeSchemeDao;
        this.extensionSchemeService = extensionSchemeService;
        this.extensionService = extensionService;
        this.codeDao = codeDao;
        this.extensionSchemeDao = extensionSchemeDao;
        this.externalReferenceDao = externalReferenceDao;
    }

    @Transactional
    public Set<CodeSchemeDTO> findAll() {
        return mapDeepCodeSchemeDtos(codeSchemeDao.findAll());
    }

    @Transactional
    public CodeSchemeDTO findById(final UUID id) {
        return mapDeepCodeSchemeDto(codeSchemeDao.findById(id));
    }

    @Transactional
    @Nullable
    public Set<CodeSchemeDTO> findByCodeRegistryCodeValue(final String codeRegistryCodeValue) {
        final Set<CodeScheme> codeSchemes = codeSchemeDao.findByCodeRegistryCodeValue(codeRegistryCodeValue);
        return mapDeepCodeSchemeDtos(codeSchemes);
    }

    @Transactional
    @Nullable
    public CodeSchemeDTO findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                                 final String codeSchemeCodeValue) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme == null) {
            return null;
        }
        return mapDeepCodeSchemeDto(codeScheme);
    }

    @Transactional
    public Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                       final String format,
                                                                       final InputStream inputStream,
                                                                       final String jsonPayload) {
        return parseAndPersistCodeSchemesFromSourceData(false, codeRegistryCodeValue, format, inputStream, jsonPayload);
    }

    @Transactional
    public Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final boolean isAuthorized,
                                                                       final String codeRegistryCodeValue,
                                                                       final String format,
                                                                       final InputStream inputStream,
                                                                       final String jsonPayload) {
        final Set<CodeScheme> codeSchemes;
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            switch (format.toLowerCase()) {
                case FORMAT_JSON:
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final Set<CodeSchemeDTO> codeSchemeDtos = codeSchemeParser.parseCodeSchemesFromJsonData(jsonPayload);
                        codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(isAuthorized, codeRegistry, codeSchemeDtos, true);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                    }
                    break;
                case FORMAT_EXCEL:
                    try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
                        final Map<CodeSchemeDTO, String> codesSheetNames = new HashMap<>();
                        final Map<CodeSchemeDTO, String> extensionSchemesSheetNames = new HashMap<>();
                        final Set<CodeSchemeDTO> codeSchemeDtos = codeSchemeParser.parseCodeSchemesFromExcelWorkbook(codeRegistry, workbook, codesSheetNames, extensionSchemesSheetNames);
                        codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(isAuthorized, codeRegistry, codeSchemeDtos, false);
                        if (codesSheetNames.isEmpty() && codeSchemes != null && codeSchemes.size() == 1 && workbook.getSheet(EXCEL_SHEET_CODES) != null) {
                            final CodeScheme codeScheme = codeSchemes.iterator().next();
                            codeService.parseAndPersistCodesFromExcelWorkbook(workbook, EXCEL_SHEET_CODES, codeScheme);
                            resolveAndSetCodeSchemeDefaultCode(codeScheme, codeSchemeDtos.iterator().next());
                        } else if (!codesSheetNames.isEmpty()) {
                            codesSheetNames.forEach((codeSchemeDto, sheetName) -> {
                                if (workbook.getSheet(sheetName) != null) {
                                    for (final CodeScheme codeScheme : codeSchemes) {
                                        if (codeScheme.getCodeValue().equalsIgnoreCase(codeSchemeDto.getCodeValue())) {
                                            codeService.parseAndPersistCodesFromExcelWorkbook(workbook, sheetName, codeScheme);
                                            resolveAndSetCodeSchemeDefaultCode(codeScheme, codeSchemeDto);
                                        }
                                    }
                                }
                            });
                        }
                        if (codeSchemes != null && !codeSchemes.isEmpty()) {
                            extensionSchemesSheetNames.forEach((codeSchemeDto, sheetName) -> {
                                for (final CodeScheme codeScheme : codeSchemes) {
                                    if (codeScheme.getCodeValue().equalsIgnoreCase(codeSchemeDto.getCodeValue())) {
                                        parseExtensionSchemes(workbook, sheetName, codeScheme);
                                    }
                                }
                            });
                        }
                    } catch (final InvalidFormatException | IOException | POIXMLException e) {
                        LOG.error("Error parsing Excel file!", e);
                        throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
                    }
                    break;
                case FORMAT_CSV:
                    codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(isAuthorized, codeRegistry, codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream), false);
                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapCodeSchemeDtos(codeSchemes, true);
    }

    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    private void parseExtensionSchemes(final Workbook workbook,
                                       final String sheetName,
                                       final CodeScheme codeScheme) {
        if (workbook.getSheet(sheetName) != null) {
            final Map<ExtensionSchemeDTO, String> extensionsSheetNames = new HashMap<>();
            final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.parseAndPersistExtensionSchemesFromExcelWorkbook(codeScheme, workbook, sheetName, extensionsSheetNames);
            if (extensionSchemes != null && !extensionSchemes.isEmpty()) {
                extensionsSheetNames.forEach((extensionSchemeDto, extensionSheetName) -> {
                    final ExtensionScheme extensionScheme = extensionSchemeDao.findById(extensionSchemeDto.getId());
                    if (extensionScheme != null) {
                        parseExtensions(workbook, extensionSheetName, extensionScheme);
                    }
                });
            }
        }
    }

    private void parseExtensions(final Workbook workbook,
                                 final String sheetName,
                                 final ExtensionScheme extensionScheme) {
        if (workbook.getSheet(sheetName) != null) {
            extensionService.parseAndPersistExtensionsFromExcelWorkbook(extensionScheme, workbook, sheetName);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    @Transactional
    public CodeSchemeDTO parseAndPersistCodeSchemeFromJson(final String codeRegistryCodeValue,
                                                           final String codeSchemeCodeValue,
                                                           final String jsonPayload) {
        CodeScheme codeScheme;
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final CodeSchemeDTO codeSchemeDto = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
                    if (!codeSchemeDto.getCodeValue().equalsIgnoreCase(codeSchemeCodeValue)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    codeScheme = codeSchemeDao.updateCodeSchemeFromDto(codeRegistry, codeSchemeDto);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistCodeSchemeFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        CodeSchemeDTO codeSchemeDTO = mapCodeSchemeDto(codeScheme, true);
        if (codeSchemeDTO.getId() != null && codeSchemeDTO.getLastCodeschemeId() != null) {
            this.populateAllVersionsToCodeSchemeDTO(codeSchemeDTO);
        }
        return codeSchemeDTO;
    }

    @Transactional
    public CodeSchemeDTO deleteCodeScheme(final String codeRegistryCodeValue,
                                          final String codeSchemeCodeValue,
                                          final Set<CodeSchemeDTO> codeSchemeDTOsToIndex) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (authorizationManager.canCodeSchemeBeDeleted(codeScheme)) {
            if (codeScheme.getRelatedExtensionSchemes() != null && !codeScheme.getRelatedExtensionSchemes().isEmpty()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_DELETE_IN_USE));
            }
            final CodeSchemeDTO codeSchemeDto = mapCodeSchemeDto(codeScheme, false);
            dealWithPossibleVersionHierarchyBeforeDeleting(codeSchemeDto, codeSchemeDTOsToIndex);
            final Set<ExternalReference> externalReferences = externalReferenceDao.findByParentCodeSchemeId(codeScheme.getId());
            if (externalReferences != null && !externalReferences.isEmpty()) {
                externalReferences.forEach(externalReference -> externalReference.setParentCodeScheme(null));
                externalReferenceDao.save(externalReferences);
                externalReferenceDao.delete(externalReferences);
            }
            codeSchemeDao.delete(codeScheme);
            return codeSchemeDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }

    private void resolveAndSetCodeSchemeDefaultCode(final CodeScheme codeScheme,
                                                    final CodeSchemeDTO codeSchemeDto) {
        if (codeSchemeDto.getDefaultCode() != null) {
            final Code defaultCode = codeDao.findByCodeSchemeAndCodeValue(codeScheme, codeSchemeDto.getDefaultCode().getCodeValue());
            if (defaultCode != null) {
                codeScheme.setDefaultCode(defaultCode);
            }
        }
    }

    @Transactional
    public CodeSchemeDTO updateCodeSchemeFromDto(final String codeRegistryCodeValue,
                                                 final CodeSchemeDTO codeSchemeDto) {
        return updateCodeSchemeFromDto(false, codeRegistryCodeValue, codeSchemeDto);
    }

    @Transactional
    public CodeSchemeDTO updateCodeSchemeFromDto(final boolean isAuthorized,
                                                 final String codeRegistryCodeValue,
                                                 final CodeSchemeDTO codeSchemeDto) {
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        final CodeScheme codeScheme = codeSchemeDao.updateCodeSchemeFromDto(isAuthorized, codeRegistry, codeSchemeDto);
        CodeSchemeDTO result = mapCodeSchemeDto(codeScheme, true);
        result.setVariantsOfThisCodeScheme(result.getVariantsOfThisCodeScheme());
        result.setVariantMothersOfThisCodeScheme(result.getVariantMothersOfThisCodeScheme());
        return result;
    }

    @Transactional
    public void populateAllVersionsToCodeSchemeDTO(final CodeSchemeDTO currentCodeScheme) {
        if (currentCodeScheme.getLastCodeschemeId() == null) {
            return;
        }
        LinkedHashSet<CodeSchemeDTO> allVersions = new LinkedHashSet<>();
        CodeSchemeDTO latestVersion;
        latestVersion = this.findById(currentCodeScheme.getLastCodeschemeId());
        allVersions = getPreviousVersions(latestVersion.getId(), allVersions);
        LinkedHashSet<CodeSchemeListItem> versionHistory = new LinkedHashSet<>();
        for (CodeSchemeDTO version : allVersions) {
            CodeSchemeListItem listItem = new CodeSchemeListItem(version.getId(), version.getPrefLabel(), version.getUri(), version.getStartDate(), version.getEndDate(), version.getStatus());
            versionHistory.add(listItem);
        }
        currentCodeScheme.setAllVersions(versionHistory);
    }

    @Transactional
    public LinkedHashSet<CodeSchemeDTO> getPreviousVersions(final UUID uuid,
                                                            final LinkedHashSet<CodeSchemeDTO> result) {
        CodeSchemeDTO prevVersion = this.findById(uuid);
        if (prevVersion == null) {
            return result;
        } else {
            result.add(prevVersion);
            if (prevVersion.getPrevCodeschemeId() == null) {
                return result;
            } else {
                return getPreviousVersions(prevVersion.getPrevCodeschemeId(), result);
            }
        }
    }

    private void dealWithPossibleVersionHierarchyBeforeDeleting(final CodeSchemeDTO currentCodeScheme,
                                                                final Set<CodeSchemeDTO> codeSchemeDTOsToIndex) {
        if (currentCodeScheme.getLastCodeschemeId() == null) {
            return; // the codescheme about to get deleted is not part of a version hierarchy, no need for actions
        }
        if (currentCodeSchemeIsOnTopOfVersionHierarchy(currentCodeScheme) && currentCodeScheme.getPrevCodeschemeId() != null) {
            dealWithTopPositionInVersionHierarchyBeforeDeletion(currentCodeScheme, codeSchemeDTOsToIndex);
        } else if (currentCodeSchemeIsSomewhereInTheMiddleOfVersionHierarchy(currentCodeScheme)) {
            dealWithMiddlePositionInVersionHierarchyBeforeDeletion(currentCodeScheme, codeSchemeDTOsToIndex);
        } else if (currentCodeSchemeIsAtTheBottomOfVersionHierarchy(currentCodeScheme)) {
            if (currentCodeScheme.getNextCodeschemeId() != null) {
                dealWithBottomPositionInVersionHierarchyBeforeDeletion(currentCodeScheme, codeSchemeDTOsToIndex);
            }
        } else {
            //should never end up here, if we do, there is a bug somewhere.
            InconsistencyInVersionHierarchyException e = new InconsistencyInVersionHierarchyException(
                new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Inconsistent version hierarchy when trying to delete codescheme.",
                    currentCodeScheme.getId().toString()));
            LOG.error("Inconsistent version hierarchy when trying to delete codescheme "
                + currentCodeScheme.getCodeValue() + " with UUID "
                + currentCodeScheme.getId() + " .", e);
        }
    }

    private boolean currentCodeSchemeIsOnTopOfVersionHierarchy(final CodeSchemeDTO currentCodeScheme) {
        return currentCodeScheme.getLastCodeschemeId().compareTo(currentCodeScheme.getId()) == 0;
    }

    private boolean currentCodeSchemeIsSomewhereInTheMiddleOfVersionHierarchy(final CodeSchemeDTO currentCodeScheme) {
        return currentCodeScheme.getLastCodeschemeId().compareTo(currentCodeScheme.getId()) != 0 &&
            currentCodeScheme.getPrevCodeschemeId() != null;
    }

    private boolean currentCodeSchemeIsAtTheBottomOfVersionHierarchy(final CodeSchemeDTO currentCodeScheme) {
        return currentCodeScheme.getPrevCodeschemeId() == null;
    }

    private void dealWithTopPositionInVersionHierarchyBeforeDeletion(final CodeSchemeDTO currentCodeScheme,
                                                                     final Set<CodeSchemeDTO> codeSchemeDTOsToIndex) {
        CodeSchemeDTO prev = this.findById(currentCodeScheme.getPrevCodeschemeId());
        prev.setLastCodeschemeId(prev.getId());
        prev.setNextCodeschemeId(null);
        prev = this.updateCodeSchemeFromDto(prev.getCodeRegistry().getCodeValue(), prev);
        this.populateAllVersionsToCodeSchemeDTO(prev);
        if (prev.getAllVersions().size() <= 1) { //if there is only one left, the version "stack" should be empty.
            prev.setAllVersions(new LinkedHashSet<>());

        }
        codeSchemeDTOsToIndex.add(prev);

        LinkedHashSet<CodeSchemeDTO> previousVersions = new LinkedHashSet<>();
        previousVersions = this.getPreviousVersions(prev.getId(), previousVersions);
        for (CodeSchemeDTO prevVersion : previousVersions) {
            prevVersion.setLastCodeschemeId(prev.getId());
            prevVersion = this.updateCodeSchemeFromDto(prevVersion.getCodeRegistry().getCodeValue(), prevVersion);
            this.populateAllVersionsToCodeSchemeDTO(prevVersion);
            if (prevVersion.getAllVersions().size() <= 1) { //if there is only one left, the version "stack" should be empty.
                prevVersion.setAllVersions(new LinkedHashSet<>());

            }
            codeSchemeDTOsToIndex.add(prevVersion);
        }
    }

    private void dealWithMiddlePositionInVersionHierarchyBeforeDeletion(final CodeSchemeDTO currentCodeScheme,
                                                                        final Set<CodeSchemeDTO> codeSchemeDTOsToIndex) {

        CodeSchemeDTO prev = this.findById(currentCodeScheme.getPrevCodeschemeId());
        prev.setNextCodeschemeId(currentCodeScheme.getNextCodeschemeId());
        prev = this.updateCodeSchemeFromDto(prev.getCodeRegistry().getCodeValue(), prev);

        LinkedHashSet<CodeSchemeDTO> olderVersions = new LinkedHashSet<>();
        olderVersions = this.getPreviousVersions(prev.getId(), olderVersions);

        CodeSchemeDTO next = this.findById(currentCodeScheme.getNextCodeschemeId());
        next.setPrevCodeschemeId(prev.getId());
        next = this.updateCodeSchemeFromDto(next.getCodeRegistry().getCodeValue(), next);

        this.populateAllVersionsToCodeSchemeDTO(prev);
        this.populateAllVersionsToCodeSchemeDTO(next);
        for (final CodeSchemeDTO olderVersion : olderVersions) {
            this.populateAllVersionsToCodeSchemeDTO(olderVersion);
        }
        codeSchemeDTOsToIndex.add(prev);
        codeSchemeDTOsToIndex.addAll(olderVersions);
        codeSchemeDTOsToIndex.add(next);

    }

    private void dealWithBottomPositionInVersionHierarchyBeforeDeletion(final CodeSchemeDTO currentCodeScheme,
                                                                        final Set<CodeSchemeDTO> codeSchemeDTOsToIndex) {
        CodeSchemeDTO next = this.findById(currentCodeScheme.getNextCodeschemeId());
        next.setPrevCodeschemeId(null);
        next = this.updateCodeSchemeFromDto(next.getCodeRegistry().getCodeValue(), next);
        this.populateAllVersionsToCodeSchemeDTO(next);
        codeSchemeDTOsToIndex.add(next);
    }
}
