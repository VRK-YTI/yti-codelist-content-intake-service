package fi.vm.yti.codelist.intake.api;

import fi.vm.yti.codelist.common.model.Meta;

public class MetaResponseWrapper {

    private Meta meta;

    public MetaResponseWrapper() {}

    public MetaResponseWrapper(final Meta meta) {
        this.meta = meta;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(final Meta meta) {
        this.meta = meta;
    }
}
