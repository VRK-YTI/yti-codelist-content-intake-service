package fi.vm.yti.codelist.intake.log;

import java.util.Set;

import javax.transaction.Transactional;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.model.ValueType;

public interface EntityChangeLogger {

    @Transactional
    void logCodeRegistryChange(final CodeRegistry codeRegistry);

    @Transactional
    void logCodeSchemeChange(final CodeScheme codeScheme);

    @Transactional
    void logCodeChange(final Code code);

    @Transactional
    void logCodesChange(final Set<Code> code);

    @Transactional
    void logExternalReferenceChange(final ExternalReference externalReference);

    @Transactional
    void logPropertyTypeChange(final PropertyType propertyType);

    @Transactional
    void logExtensionChange(final Extension extension);

    @Transactional
    void logMemberChange(final Member member);

    @Transactional
    void logMemberChanges(final Set<Member> members);

    @Transactional
    void logValueTypeChange(final ValueType member);
}
