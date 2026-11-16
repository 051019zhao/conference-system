package com.scut.conference.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 全局异常处理:把各类异常统一转换为 Result 响应,避免堆栈泄露给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常:参数/状态校验不通过 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(400, e.getMessage());
    }

    /** 参数校验失败(@Valid 注解) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数不合法" : fe.getDefaultMessage();
        return Result.fail(400, msg);
    }

    /** 上传文件缺少 file 部分 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingPart(MissingServletRequestPartException e) {
        return Result.fail(400, "请选择要上传的文件");
    }

    /** 上传文件超过大小限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.fail(400, "上传文件不能超过 20MB");
    }

    /** 数据库唯一约束冲突(用户名/邮箱/专题名重复等) */
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public Result<Void> handleDuplicate(org.springframework.dao.DuplicateKeyException e) {
        return Result.fail(400, "提交的内容与已有记录重复(如用户名、邮箱、专题名称)");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统异常,请稍后重试");
    }
}
