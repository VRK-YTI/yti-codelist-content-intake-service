package fi.vm.yti.codelist.intake.domain;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;

@Singleton
@Service
public class DomainImpl implements Domain {

    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRepository codeRepository;

    @Inject
    private DomainImpl(final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository) {
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    public void persistCodeRegistries(final List<CodeRegistry> codeRegistries) {
        codeRegistryRepository.save(codeRegistries);
    }

    public void persistCodeSchemes(final List<CodeScheme> codeSchemes) {
        codeSchemeRepository.save(codeSchemes);
    }

    public void persistCodes(final List<Code> codes) {
        codeRepository.save(codes);
    }
}
