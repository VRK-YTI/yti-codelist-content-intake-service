package fi.vm.yti.codelist.intake.dto;

public class SystemMetaCountDTO {

    private long codeRegistryCount;
    private long codeSchemeCount;
    private long codeCount;
    private long extensionCount;
    private long memberCount;

    public SystemMetaCountDTO(final long codeRegistryCount,
                              final long codeSchemeCount,
                              final long codeCount,
                              final long extensionCount,
                              final long memberCount) {
        this.codeRegistryCount = codeRegistryCount;
        this.codeSchemeCount = codeSchemeCount;
        this.codeCount = codeCount;
        this.extensionCount = extensionCount;
        this.memberCount = memberCount;
    }

    public long getCodeRegistryCount() {
        return codeRegistryCount;
    }

    public long getCodeSchemeCount() {
        return codeSchemeCount;
    }

    public long getCodeCount() {
        return codeCount;
    }

    public long getExtensionCount() {
        return extensionCount;
    }

    public long getMemberCount() {
        return memberCount;
    }
}
