package cn.projectan.strix.core.aop.advice;

import cn.projectan.strix.core.exception.StrixException;
import cn.projectan.strix.core.exception.StrixNoAuthException;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import cn.projectan.strix.core.ret.RetResult;
import cn.projectan.strix.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
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
 * 全局异常处理类
 *
 * @author ProjectAn
 * @date 2021/5/7 18:20
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public RetResult<Object> handleConstraintViolationException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        String message = allErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("、"));
        return RetMarker.makeErrRsp(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RetResult<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        return RetMarker.makeErrRsp(I18nUtil.getMessage("error.params_not_allow"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public RetResult<Object> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return RetMarker.makeRsp(RetCode.NOT_FOUND, I18nUtil.getMessage("error.api_not_found"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public RetResult<Object> handleNoResourceFoundException(NoResourceFoundException e) {
        return RetMarker.makeRsp(RetCode.NOT_FOUND, I18nUtil.getMessage("error.api_not_found"));
    }

    @ExceptionHandler(StrixNoAuthException.class)
    public RetResult<Object> handleStrixNoAuthException(StrixNoAuthException e) {
        return RetMarker.makeRsp(RetCode.NOT_LOGIN, I18nUtil.getMessage("error.not_login"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public RetResult<Object> handleAccessDeniedException(AccessDeniedException e) {
        return RetMarker.makeRsp(RetCode.NOT_PERMISSION, I18nUtil.getMessage("error.not_permission"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public RetResult<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return RetMarker.makeErrRsp(e.getMessage());
    }

    @ExceptionHandler(StrixException.class)
    public RetResult<Object> handleStrixException(StrixException e) {
        return RetMarker.makeErrRsp(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public RetResult<Object> handleException(Exception e) {
        if (e instanceof NoHandlerFoundException) {
            return RetMarker.makeRsp(RetCode.NOT_FOUND, I18nUtil.getMessage("error.api_not_found"));
        } else if (e instanceof NoResourceFoundException) {
            return RetMarker.makeRsp(RetCode.NOT_FOUND, I18nUtil.getMessage("error.api_not_found"));
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            return RetMarker.makeRsp(RetCode.METHOD_ERROR, I18nUtil.getMessage("error.api_method_unsupported"));
        }
        log.error(e.getMessage(), e);
        return RetMarker.makeErrRsp(I18nUtil.getMessage("error.other"));
    }

}
