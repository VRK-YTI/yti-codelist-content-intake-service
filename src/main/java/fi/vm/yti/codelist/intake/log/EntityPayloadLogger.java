package fi.vm.yti.codelist.intake.log;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.PropertyType;

public interface EntityPayloadLogger {

    void logCodeRegistry(final CodeRegistry codeRegistry);

    void logCodeScheme(final CodeScheme codeScheme);

    void logCode(final Code code);

    void logExternalReference(final ExternalReference externalReference);

    void logPropertyType(final PropertyType propertyType);

    void logExtension(final Extension extension);

    void logMember(final Member member);
}
