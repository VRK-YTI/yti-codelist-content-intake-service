package fi.vm.yti.codelist.intake.service.impl;

import fi.vm.yti.codelist.common.dto.*;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.*;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CloningService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
@Service
public class CloningServiceImpl extends BaseService implements CloningService {

    AuthorizationManager authorizationManager;
    CodeSchemeRepository codeSchemeRepository;
    CodeSchemeService  codeSchemeService;
    CodeService codeService;
    CodeSchemeDao codeSchemeDao;
    CodeDao codeDao;

    public CloningServiceImpl(final AuthorizationManager authorizationManager,
                              final CodeSchemeRepository codeSchemeRepository,
                              final CodeSchemeService codeSchemeService,
                              final CodeService codeService,
                              final CodeSchemeDao codeSchemeDao,
                              final CodeDao codeDao,
                              final ApiUtils apiUtils,
                              final DataSource dataSource) {
        super(apiUtils, dataSource);
        this.authorizationManager = authorizationManager;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
    }

    @Transactional
    public CodeSchemeDTO topLevelClone(CodeSchemeDTO codeSchemeWithUserChangesFromUi, String codeRegistryCodeValue, String originalCodeSchemeUuid) {

        CodeScheme originalCodeScheme = findCodeSchemeAndEagerFetchTheChildren(UUID.fromString(originalCodeSchemeUuid));

        codeSchemeWithUserChangesFromUi = codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);

        CodeScheme newCodeScheme = codeSchemeDao.findById(codeSchemeWithUserChangesFromUi.getId());

        Set<Code> originalCodes = originalCodeScheme.getCodes();
        Set<CodeDTO> clonedCodeDTOs = new HashSet<>();
        for (Code code : originalCodes) {
            Code cc = cloneCode(code, newCodeScheme);
            codeDao.save(cc);
            CodeDTO clonedCodeDTO = mapCodeDto(cc, true);
            clonedCodeDTOs.add(clonedCodeDTO);
        }
        codeSchemeWithUserChangesFromUi.setCodes(clonedCodeDTOs);

        return codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);
    }

    @Transactional
    public CodeScheme findById(final UUID id) {
        return codeSchemeRepository.findById(id);
    }

    @Transactional
    public CodeScheme findCodeSchemeAndEagerFetchTheChildren(final UUID id) {
        return codeSchemeRepository.findCodeSchemeAndEagerFetchTheChildren(id);
    }

    @Transactional
    public Code cloneCode(Code code, CodeScheme newCodeScheme) {
        Code copy = new Code();
        copy.setId(UUID.randomUUID());
        copy.setCodeScheme(newCodeScheme);
        copy.setCodeValue(code.getCodeValue());
        copy.setConceptUriInVocabularies(code.getConceptUriInVocabularies());
        copy.setBroaderCodeId(code.getBroaderCodeId());
        copy.setHierarchyLevel(code.getHierarchyLevel());
        copy.setDefinition(code.getDefinition());
        copy.setDescription(code.getDescription());
        copy.setOrder(code.getOrder());
        copy.setPrefLabel(code.getPrefLabel());
        copy.setShortName(code.getShortName());
        copy.setExternalReferences(code.getExternalReferences());
        for (ExternalReference extRef : copy.getExternalReferences()) {
            extRef.setId(null);
        }
        copy.setExtensions(code.getExtensions());
        for (Extension ext : copy.getExtensions()) {
            ext.setId(null);
        }
        copy.setStatus(Status.DRAFT.toString());
        copy.setEndDate(code.getEndDate());
        copy.setStartDate(code.getStartDate());
        copy.setUri(apiUtils.createCodeUri(copy));
        return copy;
    }

    @Transactional
    public CodeScheme cloneCodeScheme(CodeScheme c) {
        CodeScheme copy = new CodeScheme();

        copy.setCodeRegistry(c.getCodeRegistry());
        copy.setExternalReferences(c.getExternalReferences());
        for (ExternalReference extRef : copy.getExternalReferences()) {
            extRef.setId(null);
        }
        copy.setExtensionSchemes(c.getExtensionSchemes());
        for (ExtensionScheme extScheme: copy.getExtensionSchemes()) {
            extScheme.setId(null);
        }
        copy.setStatus(Status.DRAFT.toString());
        copy.setUri(apiUtils.createCodeSchemeUri(copy));

        Set<Code> originalCodes = c.getCodes();
        Set<Code> copiedCodes = new HashSet<>();
        for (Code code : originalCodes) {
            copiedCodes.add(this.cloneCode(code, copy));
        }
        copy.setCodes(copiedCodes);

        return copy;
    }
}
