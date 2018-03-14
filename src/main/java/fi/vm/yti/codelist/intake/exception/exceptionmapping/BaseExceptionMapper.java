package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import javax.ws.rs.core.Response;

import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;

public class BaseExceptionMapper {

    protected Response getResponse(final YtiCodeListException ex) {
        ResponseWrapper wrapper = new ResponseWrapper();
        Meta meta = new Meta();
        meta.setMessage(ex.getErrorModel().getMessage());
        meta.setCode(ex.getErrorModel().getHttpStatusCode());
        meta.setEntityIdentifier(ex.getErrorModel().getEntityIdentifier());
        wrapper.setMeta(meta);
        return Response.status(ex.getErrorModel().getHttpStatusCode()).entity(wrapper).build();
    }
}
