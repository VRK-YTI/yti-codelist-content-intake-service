package fi.vm.yti.codelist.intake.filter;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import fi.vm.yti.security.AuthenticatedUserProvider;

@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    private AuthenticatedUserProvider userProvider;

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        MDC.put("startTime", String.valueOf(System.currentTimeMillis()));

        LOG.debug("*** Start request logging ***");
        LOG.debug("Resource: /{}", requestContext.getUriInfo().getPath());
        LOG.debug("Class: {}", resourceInfo.getResourceClass().getCanonicalName());
        LOG.debug("Method: {}", resourceInfo.getResourceMethod().getName());
        if (!userProvider.getUser().isAnonymous()) {
            LOG.debug("User: {}", userProvider.getUser().getId());
        } else {
            LOG.debug("User: anonymous");
        }
        logQueryParameters(requestContext);
        logRequestHeader(requestContext);
    }

    private void logQueryParameters(final ContainerRequestContext requestContext) {
        LOG.debug("*** Start query parameters section of request ***");
        requestContext.getUriInfo().getQueryParameters().keySet().forEach(parameterName -> {
            final List<String> paramList = requestContext.getUriInfo().getQueryParameters().get(parameterName);
            paramList.forEach(paramValue -> LOG.debug("Parameter: {}, Value: {}", parameterName, paramValue));
        });
        LOG.debug("*** End query parameters section of request ***");
    }

    private void logRequestHeader(final ContainerRequestContext requestContext) {
        LOG.debug("*** Start header section of request ***");
        LOG.debug("Method type: {}", requestContext.getMethod());
        requestContext.getHeaders().keySet().forEach(headerName -> {
            String headerValue = requestContext.getHeaderString(headerName);
            if ("User-Agent".equalsIgnoreCase(headerName)) {
                MDC.put("userAgent", headerValue);
            } else if ("Host".equalsIgnoreCase(headerName)) {
                MDC.put("host", headerValue);
            }
            LOG.debug("Header: {}, Value: {} ", headerName, headerValue);
        });
        LOG.debug("*** End header section of request ***");
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) {
        final Long executionTime = getExecutionTime();
        if (executionTime == null) {
            return;
        }
        LOG.debug("Request execution time: {} ms", executionTime);
        LOG.debug("*** End request logging ***");
        logRequestInfo(requestContext, responseContext, executionTime);
        MDC.clear();
    }

    private Long getExecutionTime() {
        final String startTimeString = MDC.get("startTime");
        if (startTimeString != null && !startTimeString.isEmpty()) {
            final Long startTime = Long.parseLong(startTimeString);
            return System.currentTimeMillis() - startTime;
        }
        return null;
    }

    private void logRequestInfo(final ContainerRequestContext requestContext,
                                final ContainerResponseContext responseContext,
                                final long executionTime) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Request: /");
        builder.append(requestContext.getMethod());
        builder.append(" ");
        builder.append(requestContext.getUriInfo().getPath());
        builder.append(", ");
        builder.append("Status: ");
        builder.append(responseContext.getStatus());
        builder.append(", ");
        builder.append("User-Agent: ");
        builder.append(MDC.get("userAgent"));
        builder.append(", ");
        builder.append("User: ");
        if (!userProvider.getUser().isAnonymous()) {
            builder.append(userProvider.getUser().getId());
        } else {
            builder.append("anonymous");
        }
        builder.append(", ");
        builder.append("Host: ");
        builder.append(MDC.get("host"));
        builder.append(", ");
        builder.append("Time: ");
        builder.append(executionTime);
        builder.append(" ms");
        LOG.info(builder.toString());
    }
}