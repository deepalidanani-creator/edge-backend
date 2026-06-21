package com.emeritus.edge_backend.web;

import com.emeritus.edge_backend.exception.MissingTenantHeaderException;
import com.emeritus.edge_backend.exception.TenantAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TenantAccessInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private static final Pattern TENANT_PATH_PATTERN =
        Pattern.compile("^/v1/tenants/([^/]+)(?:/.*)?$");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        if (!requiresTenantHeader(path)) {
            return true;
        }

        String headerTenantId = request.getHeader(TENANT_HEADER);
        if (headerTenantId == null || headerTenantId.isBlank()) {
            throw new MissingTenantHeaderException();
        }

        Matcher matcher = TENANT_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            String pathTenantId = matcher.group(1);
            if (!pathTenantId.equals(headerTenantId)) {
                throw new TenantAccessDeniedException(
                    "Access denied: X-Tenant-Id does not match the requested tenant.");
            }
        }

        return true;
    }

    private boolean requiresTenantHeader(String path) {
        return path.startsWith("/v1/sessions")
            || path.startsWith("/v1/employees")
            || path.startsWith("/v1/tenants/")
            || path.startsWith("/v1/upload/emp-with-cohort-assignment");
    }
}
