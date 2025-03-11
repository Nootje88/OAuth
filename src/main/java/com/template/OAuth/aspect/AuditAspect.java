package com.template.OAuth.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.OAuth.annotation.Auditable;
import com.template.OAuth.enums.AuditEventType;
import com.template.OAuth.service.AuditService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    @Autowired
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Around advice to log method execution with @Auditable annotation
     */
    @Around("@annotation(com.template.OAuth.annotation.Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get method signature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Get annotation
        Auditable auditable = method.getAnnotation(Auditable.class);
        AuditEventType eventType = auditable.type();

        // Build description
        String description = auditable.description().isEmpty()
                ? method.getName()
                : auditable.description();

        long startTime = System.currentTimeMillis();
        Object result = null;
        String outcome = "SUCCESS";

        try {
            // Execute the method
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            outcome = "FAILURE: " + e.getMessage();
            throw e;
        } finally {
            // Log event after method completes or throws
            long executionTime = System.currentTimeMillis() - startTime;

            // Create details with method arguments and execution time
            Map<String, Object> detailsMap = new HashMap<>();

            // Only include arguments if flag is set
            if (auditable.includeArgs()) {
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();

                if (paramNames != null && args != null && paramNames.length == args.length) {
                    Map<String, Object> argsMap = new HashMap<>();
                    for (int i = 0; i < paramNames.length; i++) {
                        // Skip sensitive parameters
                        if (!isSensitiveParam(paramNames[i])) {
                            argsMap.put(paramNames[i], maskSensitiveData(args[i]));
                        }
                    }
                    detailsMap.put("arguments", argsMap);
                }
            }

            // Include execution time
            detailsMap.put("executionTimeMs", executionTime);

            // Include method name and class
            detailsMap.put("method", method.getName());
            detailsMap.put("class", method.getDeclaringClass().getSimpleName());

            // Include result summary if flag is set and result exists
            if (auditable.includeResult() && result != null) {
                detailsMap.put("resultType", result.getClass().getSimpleName());
                // For collections, include size
                if (result instanceof java.util.Collection) {
                    detailsMap.put("resultSize", ((java.util.Collection<?>) result).size());
                }
            }

            String details;
            try {
                details = objectMapper.writeValueAsString(detailsMap);
            } catch (JsonProcessingException e) {
                details = "Error serializing details: " + e.getMessage();
                logger.warn("Failed to serialize audit details", e);
            }

            auditService.logEvent(eventType, description, details, outcome);
        }
    }

    /**
     * Additional aspect to log exceptions
     */
    @AfterThrowing(pointcut = "execution(* com.template.OAuth.controller.*.*(..))", throwing = "exception")
    public void logException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().toShortString();
        String arguments = Arrays.stream(joinPoint.getArgs())
                .map(this::maskSensitiveData)
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        String description = "Exception in " + methodName;
        String details = "Arguments: [" + arguments + "], Exception: " + exception.getMessage();

        auditService.logEvent(AuditEventType.SYSTEM_EVENT, description, details, "EXCEPTION");
    }

    /**
     * Check if a parameter name indicates sensitive data
     */
    private boolean isSensitiveParam(String paramName) {
        String lowerParam = paramName.toLowerCase();
        return lowerParam.contains("password") ||
                lowerParam.contains("secret") ||
                lowerParam.contains("token") ||
                lowerParam.contains("key") ||
                lowerParam.contains("credential");
    }

    /**
     * Mask sensitive data in argument values
     */
    private Object maskSensitiveData(Object arg) {
        if (arg == null) {
            return null;
        }

        // Mask passwords or tokens in strings
        if (arg instanceof String) {
            String stringArg = (String) arg;
            if (stringArg.length() > 8 &&
                    (stringArg.startsWith("Bearer ") ||
                            stringArg.matches(".*[Tt]oken.*") ||
                            stringArg.matches(".*[Pp]assword.*"))) {
                return "******";
            }
            return arg;
        }

        // For complex objects, just return type name to avoid excessive logging
        if (!(arg instanceof Number || arg instanceof Boolean || arg.getClass().isEnum())) {
            return arg.getClass().getSimpleName() + " instance";
        }

        return arg;
    }
}