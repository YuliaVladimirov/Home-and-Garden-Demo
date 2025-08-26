package org.example.homeandgarden.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Service)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String argsAsString = Arrays.toString(joinPoint.getArgs());
        String layer = className.contains("Controller") ? "CONTROLLER" : "SERVICE";

        if (MDC.get("REQUEST_ID") == null) {

            MDC.put("REQUEST_ID", UUID.randomUUID().toString());
        }

        MDC.put("LAYER", layer);
        MDC.put("METHOD", methodName);

        log.info("[START] {} | args={}", className + "." + methodName, argsAsString);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("DURATION", duration + "ms");

            log.info("[END] {} | duration={} ms | result={}", className + "." + methodName, duration, result);
            return result;

        } catch (Exception exception) {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("DURATION", duration + "ms");

            log.error("[ERROR] {} | duration={} ms | args={} | message={}", className + "." + methodName, duration, argsAsString, exception.getMessage(), exception);
            throw exception;

        } finally {
            MDC.clear();
        }
    }
}
