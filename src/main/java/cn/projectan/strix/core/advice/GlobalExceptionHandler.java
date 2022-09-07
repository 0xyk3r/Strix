package cn.projectan.strix.core.advice;

import cn.projectan.strix.core.exception.StrixUniqueDetectionException;
import cn.projectan.strix.core.ret.RetCode;
import cn.projectan.strix.core.ret.RetMarker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理类
 *
 * @author 安炯奕
 * @date 2021/5/7 18:20
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Object handleConstraintViolationException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        String message = allErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("、"));
        return RetMarker.makeErrRsp(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        return RetMarker.makeErrRsp("参数非法");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Object handleNoHandlerFoundException(NoHandlerFoundException e) {
        return RetMarker.makeRsp(RetCode.NOT_FOUND, "API不存在");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException e) {
        return RetMarker.makeErrRsp(e.getMessage());
    }

    @ExceptionHandler(StrixUniqueDetectionException.class)
    public Object handleStrixUniqueDetectionException(StrixUniqueDetectionException e) {
        return RetMarker.makeErrRsp(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e) {
        if (e instanceof NoHandlerFoundException) {
            return RetMarker.makeRsp(RetCode.NOT_FOUND, "API不存在");
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            return RetMarker.makeRsp(RetCode.METHOD_ERROR, "API请求方式错误");
        }
        log.error(e.getMessage(), e);
        return RetMarker.makeErrRsp("内部错误");
    }

}
