package cn.projectan.strix.core.aop.advice;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.exception.StrixNoAuthException;
import cn.projectan.strix.core.ret.RetBuilder;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
    public ResponseEntity<RetResult<Object>> handleConstraintViolationException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        String message = allErrors.stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.error(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RetResult<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.error(I18nUtil.get("error.paramsNotAllow")));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<RetResult<Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RetResult<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound")));
    }

    @ExceptionHandler(StrixNoAuthException.class)
    public ResponseEntity<RetResult<Object>> handleStrixNoAuthException(StrixNoAuthException e) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.build(RetCode.NOT_LOGIN, I18nUtil.get("error.notLogin")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RetResult<Object>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.build(RetCode.NOT_PERMISSION, I18nUtil.get("error.notPermission")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RetResult<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.error(e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RetResult<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.error(I18nUtil.get("error.paramsNotPresent")));
    }

    @ExceptionHandler(StrixException.class)
    public ResponseEntity<RetResult<Object>> handleStrixException(StrixException e) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RetResult<Object>> handleException(Exception e) {
        if (e instanceof NoHandlerFoundException) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound")));
        } else if (e instanceof NoResourceFoundException) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(RetBuilder.build(RetCode.NOT_FOUND, I18nUtil.get("error.apiNotFound")));
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(RetBuilder.build(RetCode.METHOD_ERROR, I18nUtil.get("error.apiMethodUnsupported")));
        }
        log.error(e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(RetBuilder.error(I18nUtil.get("error.otherError")));
    }

}
