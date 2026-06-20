package com.ziyara.backend.infrastructure.aop;

import com.ziyara.backend.application.annotation.Audited;
import com.ziyara.backend.infrastructure.security.SecurityContextUserId;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Intercepts methods annotated with {@link Audited} and writes an entry to
 * the audit log after each successful execution. Failures are also logged
 * with an "ERROR" prefix on the action name.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditServiceApi auditService;

    @Around("@annotation(com.ziyara.backend.application.annotation.Audited)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        String action = audited.action();
        String entityType = audited.entityType().isBlank() ? method.getDeclaringClass().getSimpleName() : audited.entityType();
        String entityId = extractEntityId(pjp.getArgs(), audited.entityIdArgIndex());
        UUID userId = SecurityContextUserId.currentUserId().orElse(null);
        String ip = extractIp();

        try {
            Object result = pjp.proceed();
            String resultEntityId = entityId != null ? entityId : tryGetId(result);
            auditService.logAction(action, entityType, resultEntityId, userId, null, null, ip, null);
            return result;
        } catch (Throwable t) {
            auditService.logAction("ERROR_" + action, entityType, entityId, userId, null, t.getMessage(), ip, null);
            throw t;
        }
    }

    private static String extractEntityId(Object[] args, int index) {
        if (index < 0 || args == null || index >= args.length) return null;
        Object arg = args[index];
        if (arg == null) return null;
        return arg.toString();
    }

    private static String tryGetId(Object result) {
        if (result == null) return null;
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
            return request.getRemoteAddr();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
