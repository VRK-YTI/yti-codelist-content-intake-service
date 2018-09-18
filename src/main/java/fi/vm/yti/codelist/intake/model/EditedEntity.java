package fi.vm.yti.codelist.intake.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "editedentity")
public class EditedEntity {

    private UUID id;
    private Commit commit;
    private CodeRegistry codeRegistry;
    private CodeScheme codeScheme;
    private Code code;
    private ExternalReference externalReference;
    private PropertyType propertyType;
    private ExtensionScheme extensionScheme;
    private Member member;

    public EditedEntity() {
    }

    public EditedEntity(final Commit commit) {
        this.id = UUID.randomUUID();
        this.commit = commit;
    }

    @Id
    @Column(name = "id")
    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", updatable = false)
    public Commit getCommit() {
        return commit;
    }

    public void setCommit(final Commit commit) {
        this.commit = commit;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coderegistry_id")
    public CodeRegistry getCodeRegistry() {
        return codeRegistry;
    }

    public void setCodeRegistry(final CodeRegistry codeRegistry) {
        this.codeRegistry = codeRegistry;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codescheme_id")
    public CodeScheme getCodeScheme() {
        return codeScheme;
    }

    public void setCodeScheme(final CodeScheme codeScheme) {
        this.codeScheme = codeScheme;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_id")
    public Code getCode() {
        return code;
    }

    public void setCode(final Code code) {
        this.code = code;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "externalreference_id")
    public ExternalReference getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(final ExternalReference externalReference) {
        this.externalReference = externalReference;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertytype_id")
    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(final PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extensionscheme_id")
    public ExtensionScheme getExtensionScheme() {
        return extensionScheme;
    }

    public void setExtensionScheme(final ExtensionScheme extensionScheme) {
        this.extensionScheme = extensionScheme;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    public Member getMember() {
        return member;
    }

    public void setMember(final Member member) {
        this.member = member;
    }
}
