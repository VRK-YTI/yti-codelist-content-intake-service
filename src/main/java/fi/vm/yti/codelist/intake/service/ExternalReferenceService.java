package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.parser.ExternalReferenceParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_CSV;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_EXCEL;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
public class ExternalReferenceService {

    private final AuthorizationManager authorizationManager;
    private final ExternalReferenceRepository externalRefernceRepository;
    private final ExternalReferenceParser externalReferenceParser;
    private final Indexing indexing;

    @Inject
    public ExternalReferenceService(final AuthorizationManager authorizationManager,
                                    final Indexing indexing,
                                    final ExternalReferenceRepository externalReferenceRepository,
                                    final ExternalReferenceParser externalReferenceParser) {
        this.authorizationManager = authorizationManager;
        this.indexing = indexing;
        this.externalRefernceRepository = externalReferenceRepository;
        this.externalReferenceParser = externalReferenceParser;
    }

    @Transactional
    public Set<ExternalReference> parseAndPersistExternalReferencesFromSourceData(final String format,
                                                                                  final InputStream inputStream,
                                                                                  final String jsonPayload,
                                                                                  final CodeScheme codeScheme) {
        Set<ExternalReference> externalReferences;
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    externalReferences = externalReferenceParser.parseExternalReferencesFromJson(jsonPayload, codeScheme);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                }
                break;
            case FORMAT_EXCEL:
                externalReferences = externalReferenceParser.parseExternalReferencesFromExcelInputStream(inputStream);
                break;
            case FORMAT_CSV:
                externalReferences = externalReferenceParser.parseExternalReferencesFromCsvInputStream(inputStream);
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unknown format used in ExternalReferenceService: " + format));
        }
        if (externalReferences != null && !externalReferences.isEmpty()) {
            externalRefernceRepository.save(externalReferences);
        }
        return externalReferences;
    }

    @Transactional
    public ExternalReference parseAndPersistExternalReferenceFromJson(final String externalReferenceId,
                                                                      final String jsonPayload,
                                                                      final CodeScheme codeScheme) {
        final ExternalReference existingExternalReference = externalRefernceRepository.findById(UUID.fromString(externalReferenceId));
        final ExternalReference externalReference;
        if (existingExternalReference != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    externalReference = externalReferenceParser.parseExternalReferenceFromJson(jsonPayload, codeScheme);
                    if (!existingExternalReference.getId().toString().equalsIgnoreCase(externalReferenceId)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Id mismatch with API call and incoming data!"));
                    }
                    externalRefernceRepository.save(externalReference);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorConstants.ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "ExternalRefernce with ID: " + externalReferenceId + " does not exist yet, please create an ExternalReference prior to updating."));
        }
        return externalReference;
    }

    @Transactional
    public void indexExternalReference(final ExternalReference externalReference) {
        final Set<ExternalReference> externalReferences = new HashSet<>();
        externalReferences.add(externalReference);
        indexExternalReferences(externalReferences);
    }

    @Transactional
    public void indexExternalReferences(final Set<ExternalReference> externalReferences) {
        indexing.updateExternalReferences(externalReferences);
    }
}
