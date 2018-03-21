package fi.vm.yti.codelist.intake.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.JUPO_REGISTRY;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.YTI_DATACLASSIFICATION_CODESCHEME;

@Component
public class CodeSchemeService extends BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeService.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final CodeSchemeParser codeSchemeParser;
    private final CodeService codeService;
    private final ApiUtils apiUtils;

    @Inject
    public CodeSchemeService(final AuthorizationManager authorizationManager,
                             final CodeRegistryRepository codeRegistryRepository,
                             final CodeSchemeRepository codeSchemeRepository,
                             final CodeRepository codeRepository,
                             final CodeSchemeParser codeSchemeParser,
                             final CodeService codeService,
                             final ApiUtils apiUtils) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.codeSchemeParser = codeSchemeParser;
        this.codeService = codeService;
        this.apiUtils = apiUtils;
    }

    @Transactional
    public Set<CodeSchemeDTO> findAll() {
        return mapDeepCodeSchemeDtos(codeSchemeRepository.findAll());
    }

    @Transactional
    public CodeSchemeDTO findById(final UUID id) {
        return mapDeepCodeSchemeDto(codeSchemeRepository.findById(id));
    }

    @Transactional
    @Nullable
    public CodeSchemeDTO findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                                 final String codeSchemeCodeValue) {
        CodeScheme scheme = codeSchemeRepository.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (scheme == null) {
            return null;
        }
        return mapDeepCodeSchemeDto(scheme);
    }

    @Transactional
    public Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                       final String format,
                                                                       final InputStream inputStream,
                                                                       final String jsonPayload) {
        final Set<CodeScheme> codeSchemes;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            switch (format.toLowerCase()) {
                case FORMAT_JSON:
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        codeSchemes = updateCodeSchemeEntities(codeRegistry, codeSchemeParser.parseCodeSchemesFromJsonData(jsonPayload));
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                    }
                    break;
                case FORMAT_EXCEL:
                    try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
                        codeSchemes = updateCodeSchemeEntities(codeRegistry, codeSchemeParser.parseCodeSchemesFromExcelWorkbook(codeRegistry, workbook));
                        if (codeSchemes.size() == 1 && workbook.getSheet(EXCEL_SHEET_CODES) != null) {
                            final CodeScheme codeScheme = codeSchemes.iterator().next();
                            codeService.parseAndPersistCodesFromExcelWorkbook(codeRegistryCodeValue, codeScheme.getCodeValue(), workbook);
                        }
                    } catch (final InvalidFormatException | IOException | POIXMLException e) {
                        throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
                    }
                    break;
                case FORMAT_CSV:
                    codeSchemes = updateCodeSchemeEntities(codeRegistry, codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream));
                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapCodeSchemeDtos(codeSchemes, true);
    }

    @Transactional
    public CodeSchemeDTO parseAndPersistCodeSchemeFromJson(final String codeRegistryCodeValue,
                                                           final String codeSchemeCodeValue,
                                                           final String jsonPayload) {
        CodeScheme codeScheme = null;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final CodeSchemeDTO codeSchemeDto = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
                    if (!codeSchemeDto.getCodeValue().equalsIgnoreCase(codeSchemeCodeValue)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    codeScheme = updateCodeSchemeEntity(codeRegistry, codeSchemeDto);
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
        return mapCodeSchemeDto(codeScheme, true);
    }

    @Transactional
    public CodeSchemeDTO deleteCodeScheme(final String codeRegistryCodeValue,
                                          final String codeSchemeCodeValue) {
        if (authorizationManager.isSuperUser()) {
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
            final CodeSchemeDTO codeSchemeDto = mapCodeSchemeDto(codeScheme, false);
            codeSchemeRepository.delete(codeScheme);
            return codeSchemeDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }

    private CodeScheme createOrUpdateCodeScheme(final CodeRegistry codeRegistry,
                                                final CodeSchemeDTO fromCodeScheme) {
        validateCodeSchemeForCodeRegistry(fromCodeScheme);
        final CodeScheme existingCodeScheme;
        if (fromCodeScheme.getId() != null) {
            existingCodeScheme = codeSchemeRepository.findById(fromCodeScheme.getId());
            if (existingCodeScheme == null) {
                checkForExistingCodeSchemeInRegistry(codeRegistry, fromCodeScheme);
            }
        } else {
            existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, fromCodeScheme.getCodeValue());
        }
        final CodeScheme codeScheme;
        if (existingCodeScheme != null) {
            codeScheme = updateCodeScheme(codeRegistry, existingCodeScheme, fromCodeScheme);
        } else {
            codeScheme = createCodeScheme(codeRegistry, fromCodeScheme);
        }
        return codeScheme;
    }

    private void checkForExistingCodeSchemeInRegistry(final CodeRegistry codeRegistry,
                                                      final CodeSchemeDTO codeScheme) {
        final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeScheme.getCodeValue());
        if (existingCodeScheme != null) {
            throw new ExistingCodeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ALREADY_EXISTING_CODE_SCHEME, existingCodeScheme.getCodeValue()));
        }
    }

    private CodeScheme updateCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeScheme existingCodeScheme,
                                        final CodeSchemeDTO fromCodeScheme) {
        boolean hasChanges = false;
        if (!Objects.equals(existingCodeScheme.getStatus(), fromCodeScheme.getStatus())) {
            if (!authorizationManager.isSuperUser() && Status.valueOf(existingCodeScheme.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCodeScheme.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingCodeScheme.setStatus(fromCodeScheme.getStatus());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getCodeRegistry(), codeRegistry)) {
            existingCodeScheme.setCodeRegistry(codeRegistry);
            hasChanges = true;
        }
        final Set<Code> classifications = resolveDataClassificationsFromDtos(fromCodeScheme.getDataClassifications());
        if (!Objects.equals(existingCodeScheme.getDataClassifications(), classifications)) {
            if (classifications != null && !classifications.isEmpty()) {
                existingCodeScheme.setDataClassifications(classifications);
            } else {
                existingCodeScheme.setDataClassifications(null);
            }
            hasChanges = true;
        }
        final String uri = apiUtils.createCodeSchemeUri(codeRegistry, existingCodeScheme);
        if (!Objects.equals(existingCodeScheme.getUri(), uri)) {
            existingCodeScheme.setUri(uri);
            hasChanges = true;
        }
        final String url = apiUtils.createCodeSchemeUrl(codeRegistry, existingCodeScheme);
        if (!Objects.equals(existingCodeScheme.getUrl(), url)) {
            existingCodeScheme.setUrl(url);
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getSource(), fromCodeScheme.getSource())) {
            existingCodeScheme.setSource(fromCodeScheme.getSource());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getLegalBase(), fromCodeScheme.getLegalBase())) {
            existingCodeScheme.setLegalBase(fromCodeScheme.getLegalBase());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getGovernancePolicy(), fromCodeScheme.getGovernancePolicy())) {
            existingCodeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getPrefLabel(language), value)) {
                existingCodeScheme.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDescription(language), value)) {
                existingCodeScheme.setDescription(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDefinition(language), value)) {
                existingCodeScheme.setDefinition(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getChangeNote(language), value)) {
                existingCodeScheme.setChangeNote(language, value);
                hasChanges = true;
            }
        }
        if (!Objects.equals(existingCodeScheme.getVersion(), fromCodeScheme.getVersion())) {
            existingCodeScheme.setVersion(fromCodeScheme.getVersion());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getStartDate(), fromCodeScheme.getStartDate())) {
            existingCodeScheme.setStartDate(fromCodeScheme.getStartDate());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getEndDate(), fromCodeScheme.getEndDate())) {
            existingCodeScheme.setEndDate(fromCodeScheme.getEndDate());
            hasChanges = true;
        }
        if (hasChanges) {
            final Date timeStamp = new Date(System.currentTimeMillis());
            existingCodeScheme.setModified(timeStamp);
        }
        return existingCodeScheme;
    }

    private CodeScheme createCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeSchemeDTO fromCodeScheme) {
        final CodeScheme codeScheme = new CodeScheme();
        codeScheme.setCodeRegistry(codeRegistry);
        codeScheme.setDataClassifications(resolveDataClassificationsFromDtos(fromCodeScheme.getDataClassifications()));
        if (fromCodeScheme.getId() != null) {
            codeScheme.setId(fromCodeScheme.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            codeScheme.setId(uuid);
        }
        codeScheme.setCodeValue(fromCodeScheme.getCodeValue());
        codeScheme.setSource(fromCodeScheme.getSource());
        codeScheme.setLegalBase(fromCodeScheme.getLegalBase());
        codeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
        final Date timeStamp = new Date(System.currentTimeMillis());
        codeScheme.setModified(timeStamp);
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            codeScheme.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            codeScheme.setDescription(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            codeScheme.setDefinition(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            codeScheme.setChangeNote(entry.getKey(), entry.getValue());
        }
        codeScheme.setVersion(fromCodeScheme.getVersion());
        codeScheme.setStatus(fromCodeScheme.getStatus());
        codeScheme.setStartDate(fromCodeScheme.getStartDate());
        codeScheme.setEndDate(fromCodeScheme.getEndDate());
        codeScheme.setUri(apiUtils.createCodeSchemeUri(codeRegistry, codeScheme));
        codeScheme.setUrl(apiUtils.createCodeSchemeUrl(codeRegistry, codeScheme));
        return codeScheme;
    }

    private Set<Code> resolveDataClassificationsFromDtos(final Set<CodeDTO> codeDtos) {
        final Set<Code> codes;
        if (codeDtos != null && !codeDtos.isEmpty()) {
            codes = new HashSet<>();
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryCodeValueAndCodeValue(JUPO_REGISTRY, YTI_DATACLASSIFICATION_CODESCHEME);
            codeDtos.forEach(codeDto -> codes.add(codeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeDto.getCodeValue())));
        } else {
            codes = null;
        }
        return codes;
    }

    private void validateCodeSchemeForCodeRegistry(final CodeSchemeDTO codeScheme) {
        if (codeScheme.getId() != null) {
            final CodeScheme existingCodeScheme = codeSchemeRepository.findById(codeScheme.getId());
            if (existingCodeScheme != null && !existingCodeScheme.getCodeValue().equalsIgnoreCase(codeScheme.getCodeValue())) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_EXISTING_CODE_MISMATCH));
            }
        }
    }

    public CodeScheme updateCodeSchemeEntity(final CodeRegistry codeRegistry,
                                             final CodeSchemeDTO codeSchemeDto) {
        CodeScheme codeScheme = null;
        if (codeRegistry != null) {
            codeScheme = createOrUpdateCodeScheme(codeRegistry, codeSchemeDto);
        }
        codeSchemeRepository.save(codeScheme);
        codeRegistryRepository.save(codeRegistry);
        return codeScheme;
    }

    public Set<CodeScheme> updateCodeSchemeEntities(final CodeRegistry codeRegistry,
                                                    final Set<CodeSchemeDTO> codeSchemeDtos) {
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        if (codeRegistry != null) {
            for (final CodeSchemeDTO fromCodeScheme : codeSchemeDtos) {
                final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, fromCodeScheme);
                if (codeScheme != null) {
                    codeSchemes.add(codeScheme);
                }
            }
        }
        if (!codeSchemes.isEmpty()) {
            codeSchemeRepository.save(codeSchemes);
            codeRegistryRepository.save(codeRegistry);
        }
        return codeSchemes;
    }
}
