package fi.vm.yti.codelist.intake.filter;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import fi.vm.yti.codelist.intake.ServiceInitializer;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;

public class DataInitializationFilter implements ContainerRequestFilter {

    @Inject
    private ServiceInitializer serviceInitializer;

    @Override
    public void filter(final ContainerRequestContext containerRequestContext) throws YtiCodeListException {
        if (serviceInitializer == null || serviceInitializer.isInitializing()) {
            containerRequestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
        }
    }
}
