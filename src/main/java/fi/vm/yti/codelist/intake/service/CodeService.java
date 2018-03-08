package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.parser.CodeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

@Component
public class CodeService {

    private final AuthorizationManager authorizationManager;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final CodeParser codeParser;
    private final Indexing indexing;

    @Inject
    public CodeService(final AuthorizationManager authorizationManager,
                       final Indexing indexing,
                       final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository,
                       final ExternalReferenceRepository externalReferenceRepository,
                       final CodeParser codeParser) {
        this.authorizationManager = authorizationManager;
        this.indexing = indexing;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.externalReferenceRepository = externalReferenceRepository;
        this.codeRepository = codeRepository;
        this.codeParser = codeParser;
    }

    @Transactional()
    public Set<Code> parseAndPersistCodesFromSourceData(final String codeRegistryCodeValue,
                                                        final String codeSchemeCodeValue,
                                                        final String format,
                                                        final InputStream inputStream,
                                                        final String jsonPayload) {
        Set<Code> codes;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                switch (format.toLowerCase()) {
                    case FORMAT_JSON:
                        if (jsonPayload != null && !jsonPayload.isEmpty()) {
                            codes = codeParser.parseCodesFromJsonData(codeScheme, jsonPayload);
                        } else {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                        }
                        break;
                    case FORMAT_EXCEL:
                        codes = codeParser.parseCodesFromExcelInputStream(codeScheme, inputStream);
                        break;
                    case FORMAT_CSV:
                        codes = codeParser.parseCodesFromCsvInputStream(codeScheme, inputStream);
                        break;
                    default:
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unknown format used in CodeService: " + format));
                }
                if (codes != null && !codes.isEmpty()) {
                    codeRepository.save(codes);
                    codeSchemeRepository.save(codeScheme);
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeScheme with CodeValue: " + codeSchemeCodeValue + " does not exist yet, please create codeScheme first."));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return codes;
    }

    @Transactional
    public Code parseAndPersistCodeFromJson(final String codeRegistryCodeValue,
                                            final String codeSchemeCodeValue,
                                            final String codeCodeValue,
                                            final String jsonPayload) {
        Code code = null;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        code = codeParser.parseCodeFromJsonData(codeScheme, jsonPayload);
                        if (!code.getCodeValue().equalsIgnoreCase(codeCodeValue)) {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeValue mismatch with API call and incoming data!"));
                        }
                        codeRepository.save(code);
                        codeSchemeRepository.save(codeScheme);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                    }
                } catch (final YtiCodeListException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorConstants.ERR_MSG_USER_500));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeScheme with CodeValue: " + codeSchemeCodeValue + " does not exist yet, please create codeScheme first."));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return code;
    }

    public void indexCode(final Code code) {
        final Set<Code> codes = new HashSet<>();
        codes.add(code);
        indexCodes(codes);
    }

    public void indexCodes(final Set<Code> codes) {
        if (!codes.isEmpty()) {
            final CodeScheme codeScheme = codes.iterator().next().getCodeScheme();
            indexing.updateCodes(codes);
            indexing.updateCodeScheme(codeScheme);
            indexing.updateExternalReferences(externalReferenceRepository.findByParentCodeScheme(codeScheme));
        }
    }
}
