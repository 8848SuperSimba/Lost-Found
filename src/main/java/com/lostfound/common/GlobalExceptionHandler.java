package com.lostfound.common;

import com.lostfound.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? ResultCode.BAD_REQUEST.getMessage() : fieldError.getDefaultMessage();
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        Object value = ex.getValue();
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数类型错误: " + name + "=" + value);
    }

    @ExceptionHandler(MyBatisSystemException.class)
    public Result<Void> handleMyBatisSystem(MyBatisSystemException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        log.error("MyBatis 异常", ex);
        if (root instanceof CannotGetJdbcConnectionException) {
            return Result.fail(
                    ResultCode.ERROR.getCode(),
                    "无法连接数据库：请确认本机 MySQL 已启动，已执行 LF.sql 创建 lost_found 库与表，"
                            + "且 application.yml 中 spring.datasource 的 url、username、password 正确。");
        }
        return Result.fail(ResultCode.ERROR);
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public Result<Void> handleCannotGetJdbcConnection(CannotGetJdbcConnectionException ex) {
        log.error("获取 JDBC 连接失败", ex);
        return Result.fail(
                ResultCode.ERROR.getCode(),
                "无法连接数据库：请确认本机 MySQL 已启动，已执行 LF.sql 创建 lost_found 库与表，"
                        + "且 application.yml 中 spring.datasource 的 url、username、password 正确。");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("未处理的异常", ex);
        return Result.fail(ResultCode.ERROR);
    }
}
