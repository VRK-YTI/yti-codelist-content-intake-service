package fi.vm.yti.cls.intake.api;

import fi.vm.yti.cls.common.model.Meta;


public class MetaResponseWrapper {

    private Meta m_meta;


    public MetaResponseWrapper() {
    }

    public MetaResponseWrapper(final Meta meta) {
        m_meta = meta;
    }


    public Meta getMeta() {
        return m_meta;
    }

    public void setMeta(final Meta meta) {
        m_meta = meta;
    }

}
