package fi.vm.yti.codelist.intake.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.IncompleteSetOfCodesTryingToGetImportedToACumulativeCodeSchemeException;
import fi.vm.yti.codelist.intake.exception.InconsistencyInVersionHierarchyException;
import fi.vm.yti.codelist.intake.exception.TooManyCodeSchemesException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.WrongCodeSchemeInFileUploadWhenUpdatingParticularCodeSchemeException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.parser.impl.CodeSchemeParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CloningService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import fi.vm.yti.codelist.intake.service.MemberService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class CodeSchemeServiceImpl implements CodeSchemeService, AbstractBaseService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeSchemeParserImpl codeSchemeParser;
    private final CodeService codeService;
    private final ExtensionService extensionService;
    private final MemberService memberService;
    private final CodeDao codeDao;
    private final ExtensionDao extensionDao;
    private final ExternalReferenceDao externalReferenceDao;
    private final DtoMapperService dtoMapperService;
    private final CloningService cloningService;
    private final CodeSchemeRepository codeSchemeRepository;
    private final ExternalReferenceService externalReferenceService;

    @Inject
    public CodeSchemeServiceImpl(final AuthorizationManager authorizationManager,
                                 final CodeRegistryDao codeRegistryDao,
                                 final CodeSchemeDao codeSchemeDao,
                                 final CodeSchemeParserImpl codeSchemeParser,
                                 final CodeService codeService,
                                 final ExtensionService extensionService,
                                 final MemberService memberService,
                                 final CodeDao codeDao,
                                 final ExtensionDao extensionDao,
                                 final ExternalReferenceDao externalReferenceDao,
                                 final DtoMapperService dtoMapperService,
                                 @Lazy final CloningService cloningService,
                                 final CodeSchemeRepository codeSchemeRepository,
                                 final ExternalReferenceService externalReferenceService) {
        this.codeRegistryDao = codeRegistryDao;
        this.authorizationManager = authorizationManager;
        this.codeSchemeParser = codeSchemeParser;
        this.codeService = codeService;
        this.codeSchemeDao = codeSchemeDao;
        this.extensionService = extensionService;
        this.memberService = memberService;
        this.codeDao = codeDao;
        this.extensionDao = extensionDao;
        this.externalReferenceDao = externalReferenceDao;
        this.dtoMapperService = dtoMapperService;
        this.cloningService = cloningService;
        this.codeSchemeRepository = codeSchemeRepository;
        this.externalReferenceService = externalReferenceService;
    }

    @Transactional
    public Set<CodeSchemeDTO> findAll() {
        return dtoMapperService.mapDeepCodeSchemeDtos(codeSchemeDao.findAll());
    }

    @Transactional
    public CodeSchemeDTO findById(final UUID id) {
        final CodeScheme codeScheme = codeSchemeDao.findById(id);
        if (codeScheme != null) {
            return dtoMapperService.mapDeepCodeSchemeDto(codeScheme);
        }
        return null;
    }

    @Transactional
    @Nullable
    public Set<CodeSchemeDTO> findByCodeRegistryCodeValue(final String codeRegistryCodeValue) {
        final Set<CodeScheme> codeSchemes = codeSchemeDao.findByCodeRegistryCodeValue(codeRegistryCodeValue);
        return dtoMapperService.mapDeepCodeSchemeDtos(codeSchemes);
    }

    @Transactional
    @Nullable
    public CodeSchemeDTO findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                                 final String codeSchemeCodeValue) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme == null) {
            return null;
        }
        return dtoMapperService.mapDeepCodeSchemeDto(codeScheme);
    }

    @Transactional
    public Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                       final String format,
                                                                       final InputStream inputStream,
                                                                       final String jsonPayload,
                                                                       final boolean userIsCreatingANewVersionOfACodeSchene,
                                                                       final String originalCodeSchemeId,
                                                                       final boolean updatingExistingCodeScheme) {
        return parseAndPersistCodeSchemesFromSourceData(false, codeRegistryCodeValue, format, inputStream, jsonPayload, userIsCreatingANewVersionOfACodeSchene, originalCodeSchemeId, updatingExistingCodeScheme);
    }

    public boolean canANewVersionOfACodeSchemeBeCreatedFromTheIncomingFileDirectly(final String codeRegistryCodeValue,
                                                                                   final String format,
                                                                                   final InputStream inputStream) {

        boolean result = true;

        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            switch (format.toLowerCase()) {
                case FORMAT_EXCEL:
                    try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
                        final Map<CodeSchemeDTO, String> codesSheetNames = new HashMap<>();
                        final Map<CodeSchemeDTO, String> externalReferencesSheetNames = new HashMap<>();
                        final Map<CodeSchemeDTO, String> extensionsSheetNames = new HashMap<>();
                        final Set<CodeSchemeDTO> codeSchemeDTOs = codeSchemeParser.parseCodeSchemesFromExcelWorkbook(codeRegistry, workbook, codesSheetNames, externalReferencesSheetNames, extensionsSheetNames);
                        doTheValidationsAfterLoadingAndParsingTheContentsOfFile(codeRegistry, codeSchemeDTOs);
                    } catch (final InvalidFormatException | IOException | POIXMLException e) {
                        LOG.error("Error parsing Excel file!", e);
                        throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
                    } catch (final YtiCodeListException e) {
                        LOG.error("Error parsing Excel file!", e);
                        throw e;
                    }
                    break;
                case FORMAT_CSV:
                    final Set<CodeSchemeDTO> codeSchemeDTOs;
                    codeSchemeDTOs = codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream);
                    doTheValidationsAfterLoadingAndParsingTheContentsOfFile(codeRegistry, codeSchemeDTOs);
                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_INVALID_FORMAT));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODEREGISTRY_NOT_FOUND));
        }
        return result;
    }

    private void doTheValidationsAfterLoadingAndParsingTheContentsOfFile(final CodeRegistry codeRegistry,
                                                                         final Set<CodeSchemeDTO> codeSchemeDTOs) {
        if (codeSchemeDTOs.size() > 1) {
            throw new TooManyCodeSchemesException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_TOO_MANY_CODESCHEMES_IN_FILE));
        }
        CodeSchemeDTO thePotentialNewVersion = codeSchemeDTOs.iterator().next();
        CodeScheme existingCodeSchemeWithSameCodeValue = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, thePotentialNewVersion.getCodeValue());
        if (existingCodeSchemeWithSameCodeValue != null) {
            throw new TooManyCodeSchemesException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ALREADY_EXISTING_CODE_SCHEME));
        }
        if (!(thePotentialNewVersion.getStatus().equals(Status.DRAFT.toString()) ||
            thePotentialNewVersion.getStatus().equals(Status.INCOMPLETE.toString()))) {
            throw new TooManyCodeSchemesException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_NOT_VALID));
        }
    }

    private LinkedHashSet<CodeSchemeDTO> handleNewVersionCreationFromFileRelatedActivities(final Set<CodeScheme> codeSchemes,
                                                                                           final String originalCodeSchemeId) {
        if (codeSchemes.size() > 1) {
            throw new TooManyCodeSchemesException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_TOO_MANY_CODESCHEMES_IN_FILE));
        }
        CodeScheme codeScheme = codeSchemes.iterator().next();
        codeScheme.setLastCodeschemeId(codeScheme.getId());
        codeScheme.setNextCodeschemeId(null);
        codeScheme.setPrevCodeschemeId(UUID.fromString(originalCodeSchemeId));
        codeScheme.setStatus(Status.DRAFT.toString());
        codeSchemeDao.save(codeScheme);

        LinkedHashSet<CodeScheme> previousVersions = new LinkedHashSet<>();
        previousVersions = cloningService.getPreviousVersions(codeScheme.getId(), previousVersions);
        previousVersions.forEach(pv -> {
            if (pv.getId().equals(UUID.fromString(originalCodeSchemeId))) {
                pv.setNextCodeschemeId(codeScheme.getId());
            }
            pv.setLastCodeschemeId(codeScheme.getId());
            codeSchemeDao.save(pv);
        });

        CodeSchemeDTO theNewVersion = this.findById(codeScheme.getId());
        CodeSchemeListItem listItem = new CodeSchemeListItem(theNewVersion.getId(), theNewVersion.getPrefLabel(), theNewVersion.getCodeValue(), theNewVersion.getUri(), theNewVersion.getStartDate(),
            theNewVersion.getEndDate(), theNewVersion.getStatus());

        LinkedHashSet<CodeSchemeDTO> previousVersionsAsDTOs = new LinkedHashSet<>();
        previousVersionsAsDTOs = this.getPreviousVersions(codeScheme.getId(), previousVersionsAsDTOs);
        LOG.debug("LISTING ALL PREVIOUS VERSIONS IN CODESCHEMESERVICEIMPL#handleNewVersionCreationFromFileRelatedActivities 243");
        previousVersionsAsDTOs.forEach(pv -> {
            System.out.println(pv.getCodeRegistry().getCodeValue());
            System.out.println(pv.getCodeValue());
            });
        previousVersionsAsDTOs.forEach(pv -> {
            pv.getAllVersions().add(listItem);
            updateCodeSchemeFromDto(pv.getCodeRegistry().getCodeValue(), pv);
        });

        return previousVersionsAsDTOs;
    }

    @Transactional
    public Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final boolean isAuthorized,
                                                                       final String codeRegistryCodeValue,
                                                                       final String format,
                                                                       final InputStream inputStream,
                                                                       final String jsonPayload,
                                                                       final boolean userIsCreatingANewVersionOfACodeScheme,
                                                                       final String originalCodeSchemeId,
                                                                       final boolean updatingExistingCodeScheme) {
        final Set<CodeScheme> codeSchemes;
        Set<CodeSchemeDTO> otherCodeSchemeDtosThatNeedToGetIndexedInCaseANewCodeSchemeVersionWasCreated = new LinkedHashSet<>();
        Set<CodeSchemeDTO> resultingCodeSchemeSetForIndexing = new LinkedHashSet<>();
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            switch (format.toLowerCase()) {
                case FORMAT_JSON:
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final Set<CodeSchemeDTO> codeSchemeDtos = codeSchemeParser.parseCodeSchemesFromJsonData(jsonPayload);
                        codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(isAuthorized, codeRegistry, codeSchemeDtos, true);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                    }
                    break;
                case FORMAT_EXCEL:
                    try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
                        CodeScheme previousCodeScheme = null;
                        final Map<CodeSchemeDTO, String> externalReferencesSheetNames = new HashMap<>();
                        final Map<CodeSchemeDTO, String> extensionsSheetNames = new HashMap<>();
                        final Map<CodeSchemeDTO, String> codesSheetNames = new HashMap<>();
                        final Set<CodeSchemeDTO> codeSchemeDtos = codeSchemeParser.parseCodeSchemesFromExcelWorkbook(codeRegistry, workbook, codesSheetNames, externalReferencesSheetNames, extensionsSheetNames);

                        if (updatingExistingCodeScheme) {
                            ensureTheCodeschemeInTheFileIsIndeedTheIntendedTargetOfTheUpdate(originalCodeSchemeId, codeSchemeDtos);
                        }

                        if (userIsCreatingANewVersionOfACodeScheme) {
                            previousCodeScheme = codeSchemeDao.findById(UUID.fromString(originalCodeSchemeId));
                            if (previousCodeScheme.isCumulative()) {
                                codeSchemeDtos.iterator().next().setCumulative(true); // this could be wrong in the Excel, if any prev version is cumulative, it cant change back to false
                            }
                        }
                        codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(isAuthorized, codeRegistry, codeSchemeDtos, false);
                        parseExternalReferences(codeSchemes, externalReferencesSheetNames, workbook);
                        parseExternalReferencesFromDtos(codeSchemes, codeSchemeDtos);
                        Map<CodeScheme, Set<CodeDTO>> codeParsingResult = parseCodes(codeSchemes, codeSchemeDtos, codesSheetNames, workbook);
                        parseExtensions(codeSchemes, extensionsSheetNames, workbook);
                        if (userIsCreatingANewVersionOfACodeScheme) {
                            if (previousCodeScheme.isCumulative()) {
                                if (preventPossibleImplicitCodeDeletionDuringFileImport) {
                                    LinkedHashSet<CodeDTO> missingCodes = getPossiblyMissingSetOfCodesOfANewVersionOfCumulativeCodeScheme(codeService.findByCodeSchemeId(previousCodeScheme.getId()), codeParsingResult.get(codeSchemes.iterator().next()));
                                    if (!missingCodes.isEmpty()) {
                                        return handleMissingCodesOfACumulativeCodeScheme(missingCodes);
                                    }
                                }
                            }
                            otherCodeSchemeDtosThatNeedToGetIndexedInCaseANewCodeSchemeVersionWasCreated = handleNewVersionCreationFromFileRelatedActivities(codeSchemes, originalCodeSchemeId);
                        }
                    } catch (final InvalidFormatException | IOException | POIXMLException e) {
                        LOG.error("Error parsing Excel file!", e);
                        throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
                    }
                    break;
                case FORMAT_CSV:
                    final Set<CodeSchemeDTO> codeSchemeDtos = codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream);
                    if (updatingExistingCodeScheme) {
                        ensureTheCodeschemeInTheFileIsIndeedTheIntendedTargetOfTheUpdate(originalCodeSchemeId, codeSchemeDtos);
                    }

                    codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(isAuthorized, codeRegistry, codeSchemeDtos, false);

                    if (userIsCreatingANewVersionOfACodeScheme) {
                        otherCodeSchemeDtosThatNeedToGetIndexedInCaseANewCodeSchemeVersionWasCreated = handleNewVersionCreationFromFileRelatedActivities(codeSchemes, originalCodeSchemeId);
                    }

                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_INVALID_FORMAT));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODEREGISTRY_NOT_FOUND));
        }
        if (userIsCreatingANewVersionOfACodeScheme) {
            resultingCodeSchemeSetForIndexing.addAll(otherCodeSchemeDtosThatNeedToGetIndexedInCaseANewCodeSchemeVersionWasCreated);
        }

        resultingCodeSchemeSetForIndexing.addAll(dtoMapperService.mapCodeSchemeDtos(codeSchemes, true));
        return resultingCodeSchemeSetForIndexing;
    }

    /**
     * This check is here because if the user tries to update a particular codescheme from the codescheme page menu, we need to ensure the
     * codescheme in the file he is uploading is in fact the same codescheme he claims he is updating.
     */
    private void ensureTheCodeschemeInTheFileIsIndeedTheIntendedTargetOfTheUpdate(final String originalCodeSchemeId,
                                                                                  final Set<CodeSchemeDTO> codeSchemeDtos) {
        final CodeSchemeDTO newCodeScheme = codeSchemeDtos.iterator().next();
        final CodeSchemeDTO originalCodeScheme = this.findById(UUID.fromString(originalCodeSchemeId));
        if ((!newCodeScheme.getCodeValue().equals(originalCodeScheme.getCodeValue()))) {
            throw new WrongCodeSchemeInFileUploadWhenUpdatingParticularCodeSchemeException(
                new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_WRONG_CODESCHEME_IN_FILE_UPLOAD_WHEN_UPDATING_PARTICULAR_CODESCHEME)
            );
        }
    }

    @Transactional
    public LinkedHashSet<CodeSchemeDTO> handleMissingCodesOfACumulativeCodeScheme(final LinkedHashSet<CodeDTO> missingCodes) {
        StringBuilder missingCodesForScreen = new StringBuilder();
        int count = 1;
        for (CodeDTO missingCode : missingCodes) {
            missingCodesForScreen.append(missingCode.getCodeValue());
            if (count < missingCodes.size()) {
                missingCodesForScreen.append(", ");
            }
            count++;
        }

        throw new IncompleteSetOfCodesTryingToGetImportedToACumulativeCodeSchemeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
            ERR_MSG_USER_INCOMPLETE_SET_OF_CODES_TRYING_TO_GET_IMPORTED_TO_CUMULATIVE_CODE_LIST, null, missingCodesForScreen.toString()));
    }

    @Transactional
    public LinkedHashSet<CodeDTO> getPossiblyMissingSetOfCodesOfANewVersionOfCumulativeCodeScheme(final Set<CodeDTO> previousVersionsCodes,
                                                                                        final Set<CodeDTO> codeDtos) {
        LinkedHashSet<CodeDTO> missingCodes = new LinkedHashSet<>();

        previousVersionsCodes.forEach(oldCode -> {
            boolean missing = codeDtos.stream().noneMatch(newCode -> {
                return newCode.getCodeValue().equals(oldCode.getCodeValue());
            });
            if (missing) {
                missingCodes.add(oldCode);
            }
        });

        final List<CodeDTO> sorted = new ArrayList<>(missingCodes);
        sorted.sort(Comparator.comparing(code -> code.getCodeValue()));
        return new LinkedHashSet<CodeDTO>(sorted);
    }

    private void parseExternalReferences(final Set<CodeScheme> codeSchemes,
                                         final Map<CodeSchemeDTO, String> externalReferencesSheetNames,
                                         final Workbook workbook) {
        if (externalReferencesSheetNames.isEmpty() && codeSchemes != null && codeSchemes.size() == 1 && workbook.getSheet(EXCEL_SHEET_LINKS) != null) {
            final CodeScheme codeScheme = codeSchemes.iterator().next();
            externalReferenceService.parseAndPersistExternalReferencesFromExcelWorkbook(workbook, EXCEL_SHEET_LINKS, codeScheme);
            codeSchemeDao.save(codeScheme);
        } else if (!externalReferencesSheetNames.isEmpty()) {
            externalReferencesSheetNames.forEach((codeSchemeDto, sheetName) -> {
                if (workbook.getSheet(sheetName) != null) {
                    for (final CodeScheme codeScheme : codeSchemes) {
                        if (codeScheme.getCodeValue().equalsIgnoreCase(codeSchemeDto.getCodeValue())) {
                            externalReferenceService.parseAndPersistExternalReferencesFromExcelWorkbook(workbook, sheetName, codeScheme);
                            codeSchemeDao.save(codeScheme);
                        }
                    }
                }
            });
        }
    }

    private void parseExternalReferencesFromDtos(final Set<CodeScheme> codeSchemes,
                                                 final Set<CodeSchemeDTO> codeSchemeDtos) {
        if (!codeSchemeDtos.isEmpty()) {
            codeSchemeDtos.forEach(codeSchemeDto -> {
                for (final CodeScheme codeScheme : codeSchemes) {
                    if (codeScheme.getCodeValue().equalsIgnoreCase(codeSchemeDto.getCodeValue())) {
                        final Set<ExternalReference> externalReferences = findOrCreateExternalReferences(codeScheme, codeSchemeDto.getExternalReferences());
                        if (externalReferences != null && !externalReferences.isEmpty()) {
                            externalReferenceDao.save(externalReferences);
                        }
                        codeScheme.setExternalReferences(externalReferences);
                        codeSchemeDao.save(codeScheme);
                    }
                }
            });
        }
    }

    private Map<CodeScheme, Set<CodeDTO>> parseCodes(final Set<CodeScheme> codeSchemes,
                                                     final Set<CodeSchemeDTO> codeSchemeDtos,
                                                     final Map<CodeSchemeDTO, String> codesSheetNames,
                                                     final Workbook workbook) {
        Map<CodeScheme, Set<CodeDTO>> returnMap = new HashMap<>();
        if (codesSheetNames.isEmpty() && codeSchemes != null && codeSchemes.size() == 1 && workbook.getSheet(EXCEL_SHEET_CODES) != null) {
            final CodeScheme codeScheme = codeSchemes.iterator().next();
            returnMap.put(codeScheme, codeService.parseAndPersistCodesFromExcelWorkbook(workbook, EXCEL_SHEET_CODES, codeScheme));
            resolveAndSetCodeSchemeDefaultCode(codeScheme, codeSchemeDtos.iterator().next());
        } else if (!codesSheetNames.isEmpty()) {
            codesSheetNames.forEach((codeSchemeDto, sheetName) -> {
                if (workbook.getSheet(sheetName) != null) {
                    for (final CodeScheme codeScheme : codeSchemes) {
                        if (codeScheme.getCodeValue().equalsIgnoreCase(codeSchemeDto.getCodeValue())) {
                            returnMap.put(codeScheme, codeService.parseAndPersistCodesFromExcelWorkbook(workbook, sheetName, codeScheme));
                            resolveAndSetCodeSchemeDefaultCode(codeScheme, codeSchemeDto);
                        }
                    }
                }
            });
        }
        return returnMap;
    }

    private void parseExtensions(final Set<CodeScheme> codeSchemes,
                                 final Map<CodeSchemeDTO, String> extensionsSheetNames,
                                 final Workbook workbook) {
        if (codeSchemes != null && !codeSchemes.isEmpty()) {
            extensionsSheetNames.forEach((codeSchemeDto, sheetName) -> {
                for (final CodeScheme codeScheme : codeSchemes) {
                    if (codeScheme.getCodeValue().equalsIgnoreCase(codeSchemeDto.getCodeValue())) {
                        parseExtensions(workbook, sheetName, codeScheme);
                    }
                }
            });
        }
    }

    private Set<ExternalReference> findOrCreateExternalReferences(final CodeScheme codeScheme,
                                                                  final Set<ExternalReferenceDTO> externalReferenceDtos) {
        if (externalReferenceDtos != null && !externalReferenceDtos.isEmpty()) {
            final Set<ExternalReference> externalReferences = new HashSet<>();
            externalReferenceDtos.forEach(externalReferenceDto -> {
                final ExternalReference externalReference = externalReferenceDao.createOrUpdateExternalReference(false, externalReferenceDto, codeScheme);
                if (externalReference != null) {
                    externalReferences.add(externalReference);
                }
            });
            return externalReferences;
        }
        return null;
    }

    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    private void parseExtensions(final Workbook workbook,
                                 final String sheetName,
                                 final CodeScheme codeScheme) {
        if (workbook.getSheet(sheetName) != null) {
            final Map<ExtensionDTO, String> membersSheetNames = new HashMap<>();
            final Set<ExtensionDTO> extensions = extensionService.parseAndPersistExtensionsFromExcelWorkbook(codeScheme, workbook, sheetName, membersSheetNames, false);
            if (extensions != null && !extensions.isEmpty()) {
                membersSheetNames.forEach((extensionDto, memberSheetName) -> {
                    final Extension extension = extensionDao.findById(extensionDto.getId());
                    if (extension != null) {
                        parseMembers(workbook, memberSheetName, extension);
                    }
                });
            }
        }
    }

    private void parseMembers(final Workbook workbook,
                              final String sheetName,
                              final Extension extension) {
        if (workbook.getSheet(sheetName) != null) {
            memberService.parseAndPersistMembersFromExcelWorkbook(extension, workbook, sheetName);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_SHEET_NOT_FOUND));
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
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistCodeSchemeFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PARSING_ERROR));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODEREGISTRY_NOT_FOUND));
        }
        CodeSchemeDTO codeSchemeDTO = dtoMapperService.mapCodeSchemeDto(codeScheme, true);
        if (codeSchemeDTO.getId() != null && codeSchemeDTO.getLastCodeschemeId() != null) {
            this.populateAllVersionsToCodeSchemeDTO(codeSchemeDTO);
        }
        return codeSchemeDTO;
    }

    @Transactional
    public CodeSchemeDTO deleteCodeScheme(final String codeRegistryCodeValue,
                                          final String codeSchemeCodeValue,
                                          final LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null && authorizationManager.canCodeSchemeBeDeleted(codeScheme)) {
            if (isServiceClassificationCodeScheme(codeScheme) || isLanguageCodeCodeScheme(codeScheme)) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_CANNOT_BE_DELETED));
            }
            checkForExternalExtensionReferences(codeScheme);
            checkForExternalExtensionMemberReferences(codeScheme);
            checkForCodeSubCodeSchemeReferences(codeScheme);
            final CodeSchemeDTO codeSchemeDto = dtoMapperService.mapCodeSchemeDto(codeScheme, false);
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

    private Set<Extension> filterRelatedExternalExtensions(final CodeScheme codeScheme) {
        final Set<Extension> filteredExtensions = new HashSet<>();
        final Set<Extension> relatedExtensions = codeScheme.getRelatedExtensions();
        if (relatedExtensions != null) {
            for (final Extension extension : relatedExtensions) {
                if (extension.getParentCodeScheme() != codeScheme) {
                    filteredExtensions.add(extension);
                }
            }
        }
        return filteredExtensions;
    }

    private void checkForExternalExtensionReferences(final CodeScheme codeScheme) {
        final Set<Extension> relatedExternalExtensions = filterRelatedExternalExtensions(codeScheme);
        if (!relatedExternalExtensions.isEmpty()) {
            final StringBuilder identifier = new StringBuilder();
            for (final Extension relatedExtension : codeScheme.getRelatedExtensions()) {
                if (identifier.length() == 0) {
                    identifier.append(relatedExtension.getUri());
                } else {
                    identifier.append("\n");
                    identifier.append(relatedExtension.getUri());
                }
            }
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_DELETE_IN_USE, identifier.toString()));
        }
    }

    private void checkForCodeSubCodeSchemeReferences(final CodeScheme codeScheme) {
        final Set<Code> relatedCodes = codeDao.findBySubCodeScheme(codeScheme);
        if (!relatedCodes.isEmpty()) {
            final StringBuilder identifier = new StringBuilder();
            for (final Code code : relatedCodes) {
                if (identifier.length() == 0) {
                    identifier.append(code.getUri());
                } else {
                    identifier.append("\n");
                    identifier.append(code.getUri());
                }
            }
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_DELETE_IN_USE, identifier.toString()));
        }
    }

    private Set<Member> filterRelatedMembers(final CodeScheme codeScheme,
                                             final Code code) {
        final Set<Member> filteredMembers = new HashSet<>();
        final Set<Member> relatedMembers = code.getMembers();
        if (relatedMembers != null) {
            for (final Member member : relatedMembers) {
                if (codeScheme != member.getExtension().getParentCodeScheme()) {
                    filteredMembers.add(member);
                }
            }
        }
        return filteredMembers;
    }

    private void checkForExternalExtensionMemberReferences(final CodeScheme codeScheme) {
        if (codeScheme.getCodes() != null && !codeScheme.getCodes().isEmpty()) {
            final Set<Code> codes = codeScheme.getCodes();
            codes.forEach(code -> {
                final Set<Member> filteredMembers = filterRelatedMembers(codeScheme, code);
                if (!filteredMembers.isEmpty()) {
                    final StringBuilder identifier = new StringBuilder();
                    for (final Member relatedMember : filteredMembers) {
                        if (identifier.length() == 0) {
                            identifier.append(relatedMember.getUri());
                        } else {
                            identifier.append("\n");
                            identifier.append(relatedMember.getUri());
                        }
                    }
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_DELETE_IN_USE, identifier.toString()));
                }
            });
        }
    }

    private void resolveAndSetCodeSchemeDefaultCode(final CodeScheme codeScheme,
                                                    final CodeSchemeDTO codeSchemeDto) {
        if (codeSchemeDto.getDefaultCode() != null) {
            final Code defaultCode = codeDao.findByCodeSchemeAndCodeValue(codeScheme, codeSchemeDto.getDefaultCode().getCodeValue());
            if (defaultCode != null) {
                codeScheme.setDefaultCode(defaultCode);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_DEFAULTCODE_NOT_FOUND));
            }
        } else {
            codeScheme.setDefaultCode(null);
        }
        codeSchemeDao.save(codeScheme);
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
        CodeSchemeDTO result = dtoMapperService.mapCodeSchemeDto(codeScheme, true);
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
            CodeSchemeListItem listItem = new CodeSchemeListItem(version.getId(), version.getPrefLabel(), version.getCodeValue(), version.getUri(), version.getStartDate(), version.getEndDate(), version.getStatus());
            versionHistory.add(listItem);
        }
        currentCodeScheme.setAllVersions(versionHistory);
    }

    @Transactional
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public LinkedHashSet<CodeSchemeDTO> getPreviousVersions(final UUID uuid,
                                                            final LinkedHashSet<CodeSchemeDTO> result) {
        CodeSchemeDTO prevVersion = this.findById(uuid);
        if (prevVersion != null) {
            result.add(prevVersion);
            if (prevVersion.getPrevCodeschemeId() == null) {
                return result;
            } else {
                return getPreviousVersions(prevVersion.getPrevCodeschemeId(), result);
            }
        }
        return result;
    }

    private void dealWithPossibleVersionHierarchyBeforeDeleting(final CodeSchemeDTO currentCodeScheme,
                                                                final LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex) {
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
                                                                     final LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex) {
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
                                                                        final LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex) {

        CodeSchemeDTO prev = this.findById(currentCodeScheme.getPrevCodeschemeId());
        prev.setNextCodeschemeId(currentCodeScheme.getNextCodeschemeId());
        prev = this.updateCodeSchemeFromDto(prev.getCodeRegistry().getCodeValue(), prev);

        LinkedHashSet<CodeSchemeDTO> allVersions = new LinkedHashSet<>();
        allVersions = this.getPreviousVersions(currentCodeScheme.getLastCodeschemeId(), allVersions);

        CodeSchemeDTO next = this.findById(currentCodeScheme.getNextCodeschemeId());
        next.setPrevCodeschemeId(prev.getId());
        next = this.updateCodeSchemeFromDto(next.getCodeRegistry().getCodeValue(), next);

        this.populateAllVersionsToCodeSchemeDTO(prev);
        this.populateAllVersionsToCodeSchemeDTO(next);
        for (final CodeSchemeDTO aVersion : allVersions) {
            if (aVersion.getId().compareTo(next.getId()) != 0 && aVersion.getId().compareTo(prev.getId()) != 0) {
                this.populateAllVersionsToCodeSchemeDTO(aVersion);
            }
        }

        LinkedHashSet<CodeSchemeDTO> otherAffectedVersionsToIndex = new LinkedHashSet<>();
        for (CodeSchemeDTO aVersion : allVersions) {
            if (aVersion.getId().compareTo(next.getId()) != 0 && aVersion.getId().compareTo(prev.getId()) != 0) {
                otherAffectedVersionsToIndex.add(aVersion);
            }
        }

        codeSchemeDTOsToIndex.addAll(otherAffectedVersionsToIndex);
        codeSchemeDTOsToIndex.add(prev);
        codeSchemeDTOsToIndex.add(next);
    }

    private void dealWithBottomPositionInVersionHierarchyBeforeDeletion(final CodeSchemeDTO currentCodeScheme,
                                                                        final LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex) {
        CodeSchemeDTO next = this.findById(currentCodeScheme.getNextCodeschemeId());
        next.setPrevCodeschemeId(null);
        next = this.updateCodeSchemeFromDto(next.getCodeRegistry().getCodeValue(), next);
        this.populateAllVersionsToCodeSchemeDTO(next);

        LinkedHashSet<CodeSchemeDTO> allVersions = new LinkedHashSet<>();
        allVersions = this.getPreviousVersions(currentCodeScheme.getLastCodeschemeId(), allVersions);

        for (final CodeSchemeDTO aVersion : allVersions) {
            this.populateAllVersionsToCodeSchemeDTO(aVersion);
        }
        codeSchemeDTOsToIndex.addAll(allVersions);
    }
}
