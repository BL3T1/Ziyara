package com.ziyara.backend.infrastructure.web;

import com.ziyara.backend.application.locale.RequestLocaleHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.io.IOException;
import java.util.Locale;

/**
 * Sets request locale from Accept-Language and stores in RequestLocaleHolder
 * so services can return _ar content when client prefers Arabic.
 */
public class LocaleFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    @Override
    public int getOrder() {
        return ORDER;
    }

    private final AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Locale locale = localeResolver.resolveLocale(request);
            RequestLocaleHolder.setLocale(locale);
            filterChain.doFilter(request, response);
        } finally {
            RequestLocaleHolder.clear();
        }
    }
}
