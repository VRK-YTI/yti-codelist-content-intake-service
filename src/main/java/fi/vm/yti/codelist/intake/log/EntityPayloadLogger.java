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

public interface EntityPayloadLogger {

    @Transactional
    void logCodeRegistry(final CodeRegistry codeRegistry);

    @Transactional
    void logCodeScheme(final CodeScheme codeScheme);

    @Transactional
    void logCode(final Code code);

    @Transactional
    void logExternalReference(final ExternalReference externalReference);

    @Transactional
    void logPropertyType(final PropertyType propertyType);

    @Transactional
    void logExtension(final Extension extension);

    @Transactional
    void logMember(final Member member);

    @Transactional
    void logMembers(final Set<Member> members);

    @Transactional
    void logValueType(final ValueType member);
}
