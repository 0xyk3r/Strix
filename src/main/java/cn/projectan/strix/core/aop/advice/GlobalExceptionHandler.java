package cn.projectan.strix.core.aop.advice;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.exception.StrixNoAuthException;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * @author ProjectAn
 * @since 2021/5/7 18:20
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public RetResult<Object> handleConstraintViolationException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        String message = allErrors.stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
        return RetBuilder.error(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RetResult<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        return RetBuilder.error(I18nUtil.get("error.paramsNotAllow"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public RetResult<Object> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public RetResult<Object> handleNoResourceFoundException(NoResourceFoundException e) {
        return RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound"));
    }

    @ExceptionHandler(StrixNoAuthException.class)
    public RetResult<Object> handleStrixNoAuthException(StrixNoAuthException e) {
        return RetBuilder.build(RetCode.NOT_LOGIN, I18nUtil.get("error.notLogin"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public RetResult<Object> handleAccessDeniedException(AccessDeniedException e) {
        return RetBuilder.build(RetCode.NOT_PERMISSION, I18nUtil.get("error.notPermission"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public RetResult<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return RetBuilder.error(e.getMessage());
    }

    @ExceptionHandler(StrixException.class)
    public RetResult<Object> handleStrixException(StrixException e) {
        return RetBuilder.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public RetResult<Object> handleException(Exception e) {
        if (e instanceof NoHandlerFoundException) {
            return RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound"));
        } else if (e instanceof NoResourceFoundException) {
            return RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound"));
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            return RetBuilder.build(RetCode.METHOD_ERROR, I18nUtil.get("error.apiMethodUnsupported"));
        }
        log.error(e.getMessage(), e);
        return RetBuilder.error(I18nUtil.get("error.otherError"));
    }

}
