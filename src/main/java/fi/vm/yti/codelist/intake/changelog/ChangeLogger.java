package fi.vm.yti.codelist.intake.changelog;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;

public interface ChangeLogger {

    void logCodeSchemeChange(final CodeScheme codeSchemeId);

    void logCodeChange(final Code codeId);

    void logExternalReferenceChange(final ExternalReference externalReferenceId);
}
